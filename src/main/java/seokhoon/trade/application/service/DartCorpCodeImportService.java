package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.DartCorpCodeImportUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.DartProperties;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.stock.Market;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class DartCorpCodeImportService implements DartCorpCodeImportUseCase {
    private final DartCorpCodeProviderPort providerPort;
    private final DartCorpMappingPort mappingPort;
    private final DartCorpCodeImportHistoryPort historyPort;
    private final DartProperties properties;
    private final OperationalMetricsPort metrics;
    private final DartCorpCodeXmlParser parser;
    private final Clock clock;

    @Autowired
    public DartCorpCodeImportService(
            DartCorpCodeProviderPort providerPort,
            DartCorpMappingPort mappingPort,
            DartCorpCodeImportHistoryPort historyPort,
            DartProperties properties,
            OperationalMetricsPort metrics
    ) {
        this(providerPort, mappingPort, historyPort, properties, metrics,
                new DartCorpCodeXmlParser(), Clock.systemUTC());
    }

    DartCorpCodeImportService(
            DartCorpCodeProviderPort providerPort,
            DartCorpMappingPort mappingPort,
            DartCorpCodeImportHistoryPort historyPort,
            DartProperties properties,
            OperationalMetricsPort metrics,
            DartCorpCodeXmlParser parser,
            Clock clock
    ) {
        this.providerPort = providerPort;
        this.mappingPort = mappingPort;
        this.historyPort = historyPort;
        this.properties = properties;
        this.metrics = metrics;
        this.parser = parser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DartCorpCodeImportHistory importCorpCodes() {
        Instant requestedAt = clock.instant();
        if (!properties.isCorpCodeImportEnabled()) {
            return save(DartCorpCodeImportStatus.SKIPPED, 0, 0,
                    "DART corp code import disabled", requestedAt);
        }
        try {
            properties.validateCorpCodeImportRequest();
            return importCorpCodesFromFile(providerPort.fetchCorpCodeFile(), requestedAt);
        } catch (RuntimeException exception) {
            return save(DartCorpCodeImportStatus.FAILED, 0, 0,
                    sanitize(exception.getMessage()), requestedAt);
        }
    }

    @Override
    @Transactional
    public DartCorpCodeImportHistory importCorpCodesFromFile(byte[] content) {
        return importCorpCodesFromFile(content, clock.instant());
    }

    private DartCorpCodeImportHistory importCorpCodesFromFile(byte[] content, Instant requestedAt) {
        try {
            List<DartCorpCodeRecord> records = parser.parse(content);
            int imported = 0;
            int matched = 0;
            int skipped = 0;
            Instant now = clock.instant();
            for (DartCorpCodeRecord record : records) {
                if (record.corpCode().isBlank() || record.corpName().isBlank()) {
                    skipped++;
                    continue;
                }
                imported++;
                if (properties.isCorpCodeImportAutoMatchListedOnly() && record.stockCode().isBlank()) {
                    skipped++;
                    continue;
                }
                if (record.stockCode().isBlank()) {
                    skipped++;
                    continue;
                }
                Market market = mappingPort.findByStockCode(record.stockCode())
                        .map(DartCorpMapping::market)
                        .orElse(Market.UNKNOWN);
                mappingPort.save(new DartCorpMapping(null, record.stockCode(),
                        record.corpCode(), record.corpName(), market, now, now));
                matched++;
            }
            DartCorpCodeImportStatus status = skipped == 0 ? DartCorpCodeImportStatus.SUCCESS
                    : matched > 0 ? DartCorpCodeImportStatus.PARTIAL : DartCorpCodeImportStatus.SKIPPED;
            String reason = skipped == 0 ? null : "skipped=" + skipped;
            return save(status, imported, matched, reason, requestedAt);
        } catch (RuntimeException exception) {
            return save(DartCorpCodeImportStatus.FAILED, 0, 0,
                    sanitize(exception.getMessage()), requestedAt);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DartCorpCodeImportHistory> findCorpCodeImportHistories() {
        return historyPort.findAllCorpCodeImports();
    }

    private DartCorpCodeImportHistory save(
            DartCorpCodeImportStatus status,
            int importedCount,
            int matchedStockCount,
            String reason,
            Instant requestedAt
    ) {
        DartCorpCodeImportHistory saved = historyPort.save(new DartCorpCodeImportHistory(
                null, status, importedCount, matchedStockCount,
                reason == null || reason.isBlank() ? null : reason, requestedAt, clock.instant()));
        metrics.recordDartCorpCodeImport(metricResult(status));
        return saved;
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "DART corp code import failed";
        }
        String sanitized = message;
        String url = properties.getCorpCodeZipUrl();
        if (url != null && !url.isBlank()) {
            sanitized = sanitized.replace(url, "[REDACTED_URL]");
        }
        String apiKey = properties.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            sanitized = sanitized.replace(apiKey, "[REDACTED]");
        }
        sanitized = sanitized.replaceAll("[\\r\\n\\t]", " ");
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }

    private static String metricResult(DartCorpCodeImportStatus status) {
        return status == DartCorpCodeImportStatus.FAILED ? "failure" : status.name().toLowerCase();
    }
}
