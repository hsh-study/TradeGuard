package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.ImportMarketIndexUseCase;
import seokhoon.trade.application.port.in.ResearchUseCases.MarketIndexUseCase;
import seokhoon.trade.application.port.in.ResearchUseCases.SaveMarketIndexCommand;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.MarketIndexProviderProperties;
import seokhoon.trade.domain.market.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class MarketIndexService implements MarketIndexUseCase, ImportMarketIndexUseCase {
    private final MarketIndexPort marketIndexPort;
    private final MarketIndexProviderPort providerPort;
    private final MarketIndexImportHistoryPort historyPort;
    private final MarketIndexProviderProperties properties;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public MarketIndexService(
            MarketIndexPort marketIndexPort,
            MarketIndexProviderPort providerPort,
            MarketIndexImportHistoryPort historyPort,
            MarketIndexProviderProperties properties,
            OperationalMetricsPort metrics
    ) {
        this(marketIndexPort, providerPort, historyPort, properties, metrics, Clock.systemUTC());
    }

    MarketIndexService(
            MarketIndexPort marketIndexPort,
            MarketIndexProviderPort providerPort,
            MarketIndexImportHistoryPort historyPort,
            MarketIndexProviderProperties properties,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.marketIndexPort = marketIndexPort;
        this.providerPort = providerPort;
        this.historyPort = historyPort;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MarketIndex save(SaveMarketIndexCommand command) {
        Objects.requireNonNull(command, "command");
        MarketIndex saved = saveIndex(command);
        saveHistory(MarketIndexImportProvider.MANUAL, command.tradeDate(),
                MarketIndexImportStatus.SUCCESS, 1, null, clock.instant());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketIndex> findByTradeDate(LocalDate tradeDate) {
        return marketIndexPort.findByTradeDate(tradeDate);
    }

    @Override
    @Transactional
    public MarketIndexImportHistory importCsv(String csv) {
        Instant requestedAt = clock.instant();
        LocalDate tradeDate = LocalDate.now(clock);
        try {
            CsvImportResult result = importRows(csv);
            tradeDate = result.tradeDate().orElse(tradeDate);
            MarketIndexImportStatus status = result.invalidRows() == 0
                    ? MarketIndexImportStatus.SUCCESS
                    : result.importedCount() > 0 ? MarketIndexImportStatus.PARTIAL
                    : MarketIndexImportStatus.FAILED;
            String reason = result.invalidRows() == 0 ? null : "invalidRows=" + result.invalidRows();
            return saveHistory(MarketIndexImportProvider.CSV, tradeDate, status,
                    result.importedCount(), reason, requestedAt);
        } catch (RuntimeException exception) {
            return saveHistory(MarketIndexImportProvider.CSV, tradeDate,
                    MarketIndexImportStatus.FAILED, 0, sanitize(exception.getMessage()), requestedAt);
        }
    }

    @Override
    @Transactional
    public MarketIndexImportHistory importProvider(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        Instant requestedAt = clock.instant();
        MarketIndexImportProvider provider = provider();
        if (!properties.isEnabled()) {
            return saveHistory(provider, tradeDate, MarketIndexImportStatus.SKIPPED,
                    0, "MARKET_INDEX_PROVIDER_DISABLED", requestedAt);
        }
        try {
            List<MarketIndex> indices = providerPort.fetchMajorIndices(tradeDate);
            int imported = 0;
            for (MarketIndex index : indices) {
                marketIndexPort.save(index);
                imported++;
            }
            MarketIndexImportStatus status = imported == 0
                    ? MarketIndexImportStatus.SKIPPED : MarketIndexImportStatus.SUCCESS;
            String reason = imported == 0 ? "NO_PROVIDER_DATA" : null;
            return saveHistory(provider, tradeDate, status, imported, reason, requestedAt);
        } catch (RuntimeException exception) {
            return saveHistory(provider, tradeDate, MarketIndexImportStatus.FAILED,
                    0, sanitize(exception.getMessage()), requestedAt);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketIndexImportHistory> findMarketIndexImportHistories() {
        return historyPort.findRecentMarketIndexImports(100);
    }

    private CsvImportResult importRows(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("CSV is empty");
        }
        List<String> lines = csv.lines().filter(line -> !line.isBlank()).toList();
        if (lines.size() < 2) {
            throw new IllegalArgumentException("CSV must contain header and rows");
        }
        Map<String, Integer> header = header(lines.get(0));
        int imported = 0;
        int invalid = 0;
        LocalDate firstTradeDate = null;
        for (int i = 1; i < lines.size(); i++) {
            try {
                List<String> columns = split(lines.get(i));
                LocalDate tradeDate = LocalDate.parse(value(columns, header, "tradeDate"));
                if (firstTradeDate == null) {
                    firstTradeDate = tradeDate;
                }
                saveIndex(new SaveMarketIndexCommand(
                        value(columns, header, "indexCode"),
                        value(columns, header, "indexName"),
                        tradeDate,
                        new BigDecimal(value(columns, header, "closePrice")),
                        new BigDecimal(value(columns, header, "changeRate")),
                        new BigDecimal(value(columns, header, "tradingValue"))
                ));
                imported++;
            } catch (RuntimeException exception) {
                invalid++;
            }
        }
        return new CsvImportResult(imported, invalid, Optional.ofNullable(firstTradeDate));
    }

    private MarketIndex saveIndex(SaveMarketIndexCommand command) {
        return marketIndexPort.save(new MarketIndex(null, command.indexCode(), command.indexName(),
                command.tradeDate(), command.closePrice(), command.changeRate(),
                command.tradingValue(), clock.instant(), clock.instant()));
    }

    private MarketIndexImportHistory saveHistory(
            MarketIndexImportProvider provider,
            LocalDate tradeDate,
            MarketIndexImportStatus status,
            int imported,
            String reason,
            Instant requestedAt
    ) {
        MarketIndexImportHistory saved = historyPort.save(new MarketIndexImportHistory(
                null, provider, tradeDate, status, imported,
                reason == null || reason.isBlank() ? null : reason, requestedAt, clock.instant()));
        metrics.recordMarketIndexImport(provider.name(), metricResult(status));
        return saved;
    }

    private MarketIndexImportProvider provider() {
        try {
            return MarketIndexImportProvider.valueOf(
                    Optional.ofNullable(properties.getType()).orElse("KIS").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return MarketIndexImportProvider.KIS;
        }
    }

    private static Map<String, Integer> header(String line) {
        List<String> columns = split(line);
        Map<String, Integer> header = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            header.put(columns.get(i), i);
        }
        for (String required : List.of("indexCode", "indexName", "tradeDate",
                "closePrice", "changeRate", "tradingValue")) {
            if (!header.containsKey(required)) {
                throw new IllegalArgumentException("Missing CSV column: " + required);
            }
        }
        return header;
    }

    private static String value(List<String> columns, Map<String, Integer> header, String name) {
        int index = header.get(name);
        if (index >= columns.size() || columns.get(index).isBlank()) {
            throw new IllegalArgumentException("Missing CSV value: " + name);
        }
        return columns.get(index);
    }

    private static List<String> split(String line) {
        return Arrays.stream(line.split(",", -1)).map(String::trim).toList();
    }

    private static String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "market index import failed";
        }
        String sanitized = message.replaceAll("[\\r\\n\\t]", " ");
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }

    private static String metricResult(MarketIndexImportStatus status) {
        return status == MarketIndexImportStatus.FAILED ? "failure" : status.name().toLowerCase(Locale.ROOT);
    }

    private record CsvImportResult(int importedCount, int invalidRows, Optional<LocalDate> tradeDate) {
    }
}
