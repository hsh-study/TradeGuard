package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.ImportSectorSeedUseCase;
import seokhoon.trade.application.port.in.ResearchUseCases.SectorUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.ResearchProperties;
import seokhoon.trade.domain.market.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class SectorSeedImportService implements ImportSectorSeedUseCase {
    private final SectorPort sectorPort;
    private final StockSectorMappingPort mappingPort;
    private final SectorImportHistoryPort historyPort;
    private final SectorUseCase sectorUseCase;
    private final MarketCalendarPort calendarPort;
    private final ResearchProperties properties;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public SectorSeedImportService(
            SectorPort sectorPort,
            StockSectorMappingPort mappingPort,
            SectorImportHistoryPort historyPort,
            SectorUseCase sectorUseCase,
            MarketCalendarPort calendarPort,
            ResearchProperties properties,
            OperationalMetricsPort metrics
    ) {
        this(sectorPort, mappingPort, historyPort, sectorUseCase, calendarPort,
                properties, metrics, Clock.systemUTC());
    }

    SectorSeedImportService(
            SectorPort sectorPort,
            StockSectorMappingPort mappingPort,
            SectorImportHistoryPort historyPort,
            SectorUseCase sectorUseCase,
            MarketCalendarPort calendarPort,
            ResearchProperties properties,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.sectorPort = sectorPort;
        this.mappingPort = mappingPort;
        this.historyPort = historyPort;
        this.sectorUseCase = sectorUseCase;
        this.calendarPort = calendarPort;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SectorImportHistory importCsv(String csv) {
        Instant requestedAt = clock.instant();
        try {
            CsvImportResult result = importRows(csv);
            SectorImportStatus status = result.invalidRows() == 0
                    ? SectorImportStatus.SUCCESS
                    : result.importedSectorCount() > 0 ? SectorImportStatus.PARTIAL
                    : SectorImportStatus.FAILED;
            String reason = result.invalidRows() == 0 ? null : "invalidRows=" + result.invalidRows();
            SectorImportHistory saved = save(status, result.importedSectorCount(),
                    result.importedMappingCount(), reason, requestedAt);
            if (status != SectorImportStatus.FAILED
                    && properties.isSectorImportAutoGenerateSnapshot()) {
                LocalDate targetDate = calendarPort.previousTradingDay(LocalDate.now(clock));
                sectorUseCase.generateSnapshots(targetDate);
            }
            return saved;
        } catch (RuntimeException exception) {
            return save(SectorImportStatus.FAILED, 0, 0,
                    sanitize(exception.getMessage()), requestedAt);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectorImportHistory> findSectorImportHistories() {
        return historyPort.findRecentSectorImports(100);
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
        Set<String> importedSectors = new HashSet<>();
        int mappings = 0;
        int invalid = 0;
        Instant now = clock.instant();
        for (int i = 1; i < lines.size(); i++) {
            try {
                List<String> columns = split(lines.get(i));
                String sectorCode = value(columns, header, "sectorCode");
                String sectorName = value(columns, header, "sectorName");
                SectorType sectorType = optional(columns, header, "sectorType")
                        .filter(value -> !value.isBlank())
                        .map(value -> SectorType.valueOf(value.toUpperCase(Locale.ROOT)))
                        .orElse(SectorType.CUSTOM);
                sectorPort.save(new Sector(null, sectorCode, sectorName, sectorType, now, now));
                importedSectors.add(sectorCode);
                Optional<String> stockCode = optional(columns, header, "stockCode")
                        .filter(value -> !value.isBlank());
                if (stockCode.isPresent()) {
                    String source = optional(columns, header, "source")
                            .filter(value -> !value.isBlank())
                            .orElse("CSV");
                    mappingPort.save(new StockSectorMapping(null, stockCode.get(),
                            sectorCode, source, now, now));
                    mappings++;
                }
            } catch (RuntimeException exception) {
                invalid++;
            }
        }
        return new CsvImportResult(importedSectors.size(), mappings, invalid);
    }

    private SectorImportHistory save(
            SectorImportStatus status,
            int sectors,
            int mappings,
            String reason,
            Instant requestedAt
    ) {
        SectorImportHistory saved = historyPort.save(new SectorImportHistory(null, status,
                sectors, mappings, reason == null || reason.isBlank() ? null : reason,
                requestedAt, clock.instant()));
        metrics.recordSectorImport(metricResult(status));
        return saved;
    }

    private static Map<String, Integer> header(String line) {
        List<String> columns = split(line);
        Map<String, Integer> header = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            header.put(columns.get(i), i);
        }
        for (String required : List.of("sectorCode", "sectorName", "sectorType", "stockCode", "source")) {
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

    private static Optional<String> optional(List<String> columns, Map<String, Integer> header, String name) {
        int index = header.get(name);
        if (index >= columns.size()) {
            return Optional.empty();
        }
        return Optional.of(columns.get(index));
    }

    private static List<String> split(String line) {
        return Arrays.stream(line.split(",", -1)).map(String::trim).toList();
    }

    private static String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "sector import failed";
        }
        String sanitized = message.replaceAll("[\\r\\n\\t]", " ");
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }

    private static String metricResult(SectorImportStatus status) {
        return status == SectorImportStatus.FAILED ? "failure" : status.name().toLowerCase(Locale.ROOT);
    }

    private record CsvImportResult(int importedSectorCount, int importedMappingCount, int invalidRows) {
    }
}
