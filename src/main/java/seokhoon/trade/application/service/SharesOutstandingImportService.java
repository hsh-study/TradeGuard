package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.GenerateValuationSnapshotUseCase;
import seokhoon.trade.application.port.in.ImportSharesOutstandingUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.ResearchProperties;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class SharesOutstandingImportService implements ImportSharesOutstandingUseCase {
    private final SharesOutstandingSnapshotPort sharesPort;
    private final SharesOutstandingImportHistoryPort historyPort;
    private final GenerateValuationSnapshotUseCase valuationUseCase;
    private final ResearchProperties properties;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public SharesOutstandingImportService(
            SharesOutstandingSnapshotPort sharesPort,
            SharesOutstandingImportHistoryPort historyPort,
            GenerateValuationSnapshotUseCase valuationUseCase,
            ResearchProperties properties,
            OperationalMetricsPort metrics
    ) {
        this(sharesPort, historyPort, valuationUseCase, properties, metrics, Clock.systemUTC());
    }

    SharesOutstandingImportService(
            SharesOutstandingSnapshotPort sharesPort,
            SharesOutstandingImportHistoryPort historyPort,
            GenerateValuationSnapshotUseCase valuationUseCase,
            ResearchProperties properties,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.sharesPort = sharesPort;
        this.historyPort = historyPort;
        this.valuationUseCase = valuationUseCase;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SharesOutstandingImportHistory importCsv(String csv) {
        Instant requestedAt = clock.instant();
        try {
            CsvImportResult result = importRows(csv);
            SharesOutstandingImportStatus status = result.invalidRows() == 0
                    ? SharesOutstandingImportStatus.SUCCESS
                    : result.importedCount() > 0 ? SharesOutstandingImportStatus.PARTIAL
                    : SharesOutstandingImportStatus.FAILED;
            String reason = result.invalidRows() == 0 ? null : "invalidRows=" + result.invalidRows();
            return save(status, result.importedCount(), reason, requestedAt);
        } catch (RuntimeException exception) {
            return save(SharesOutstandingImportStatus.FAILED, 0,
                    sanitize(exception.getMessage()), requestedAt);
        }
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
        Instant now = clock.instant();
        for (int i = 1; i < lines.size(); i++) {
            try {
                List<String> columns = split(lines.get(i));
                String stockCode = value(columns, header, "stockCode");
                LocalDate baseDate = LocalDate.parse(value(columns, header, "baseDate"));
                BigDecimal shares = new BigDecimal(value(columns, header, "sharesOutstanding"));
                SharesOutstandingSource source = SharesOutstandingSource.valueOf(
                        value(columns, header, "source").toUpperCase(Locale.ROOT));
                sharesPort.save(new SharesOutstandingSnapshot(null, stockCode, baseDate,
                        shares, source, now, now));
                imported++;
                if (properties.isSharesOutstandingImportAutoGenerateValuation()) {
                    valuationUseCase.generate(stockCode, baseDate);
                }
            } catch (RuntimeException exception) {
                invalid++;
            }
        }
        return new CsvImportResult(imported, invalid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SharesOutstandingImportHistory> findSharesOutstandingImportHistories() {
        return historyPort.findAllSharesOutstandingImports();
    }

    private SharesOutstandingImportHistory save(
            SharesOutstandingImportStatus status,
            int importedCount,
            String reason,
            Instant requestedAt
    ) {
        SharesOutstandingImportHistory saved = historyPort.save(new SharesOutstandingImportHistory(
                null, status, importedCount,
                reason == null || reason.isBlank() ? null : reason, requestedAt, clock.instant()));
        metrics.recordSharesOutstandingImport(metricResult(status));
        return saved;
    }

    private static Map<String, Integer> header(String line) {
        List<String> columns = split(line);
        Map<String, Integer> header = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            header.put(columns.get(i), i);
        }
        for (String required : List.of("stockCode", "baseDate", "sharesOutstanding", "source")) {
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
            return "shares outstanding import failed";
        }
        String sanitized = message.replaceAll("[\\r\\n\\t]", " ");
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }

    private static String metricResult(SharesOutstandingImportStatus status) {
        return status == SharesOutstandingImportStatus.FAILED ? "failure" : status.name().toLowerCase();
    }

    private record CsvImportResult(int importedCount, int invalidRows) {
    }
}
