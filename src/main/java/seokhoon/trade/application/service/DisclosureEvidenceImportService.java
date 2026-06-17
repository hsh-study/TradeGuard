package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.ImportDisclosureEvidenceUseCase;
import seokhoon.trade.application.port.out.DisclosureEvidenceImportHistoryPort;
import seokhoon.trade.application.port.out.DisclosureEvidenceProviderPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.config.DisclosureProviderProperties;
import seokhoon.trade.domain.research.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class DisclosureEvidenceImportService implements ImportDisclosureEvidenceUseCase {
    private final DisclosureEvidenceProviderPort providerPort;
    private final DisclosureEvidenceImportHistoryPort historyPort;
    private final CatalystEvidenceService evidenceService;
    private final DisclosureProviderProperties properties;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public DisclosureEvidenceImportService(
            DisclosureEvidenceProviderPort providerPort,
            DisclosureEvidenceImportHistoryPort historyPort,
            CatalystEvidenceService evidenceService,
            DisclosureProviderProperties properties,
            OperationalMetricsPort metrics
    ) {
        this(providerPort, historyPort, evidenceService, properties, metrics, Clock.systemUTC());
    }

    DisclosureEvidenceImportService(
            DisclosureEvidenceProviderPort providerPort,
            DisclosureEvidenceImportHistoryPort historyPort,
            CatalystEvidenceService evidenceService,
            DisclosureProviderProperties properties,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.providerPort = providerPort;
        this.historyPort = historyPort;
        this.evidenceService = evidenceService;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DisclosureEvidenceImportHistory importDisclosures(String stockCode, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        Instant requestedAt = clock.instant();
        if (!properties.isEnabled()) {
            return save(stockCode, from, to, DisclosureEvidenceImportStatus.SKIPPED, 0,
                    "disclosure provider disabled", requestedAt);
        }
        try {
            List<DisclosureEvidenceRecord> records = providerPort.fetchDisclosures(stockCode, from, to);
            records.forEach(evidenceService::saveProviderEvidence);
            DisclosureEvidenceImportStatus status = records.isEmpty()
                    ? DisclosureEvidenceImportStatus.SKIPPED : DisclosureEvidenceImportStatus.SUCCESS;
            return save(stockCode, from, to, status, records.size(), null, requestedAt);
        } catch (RuntimeException exception) {
            return save(stockCode, from, to, DisclosureEvidenceImportStatus.FAILED, 0,
                    sanitize(exception.getMessage()), requestedAt);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisclosureEvidenceImportHistory> findDisclosureImportHistories() {
        return historyPort.findRecentDisclosureImports(100);
    }

    private DisclosureEvidenceImportHistory save(
            String stockCode,
            LocalDate from,
            LocalDate to,
            DisclosureEvidenceImportStatus status,
            int importedCount,
            String reason,
            Instant requestedAt
    ) {
        DisclosureEvidenceImportHistory saved = historyPort.save(new DisclosureEvidenceImportHistory(
                null, properties.getType(), stockCode, from, to, status, importedCount,
                reason == null || reason.isBlank() ? null : reason, requestedAt, clock.instant()));
        metrics.recordDisclosureEvidenceImport(properties.getType().name(), metricResult(status));
        return saved;
    }

    private static String metricResult(DisclosureEvidenceImportStatus status) {
        return status == DisclosureEvidenceImportStatus.FAILED ? "failure" : status.name().toLowerCase();
    }

    private static String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "disclosure evidence import failed";
        }
        String sanitized = message.replaceAll("[\\r\\n\\t]", " ");
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }
}
