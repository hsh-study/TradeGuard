package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.DisclosureProviderProperties;
import seokhoon.trade.domain.research.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DisclosureEvidenceImportServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Test
    void recordsSkippedWhenProviderIsDisabled() {
        InMemoryHistoryPort histories = new InMemoryHistoryPort();
        DisclosureProviderProperties properties = new DisclosureProviderProperties();
        DisclosureEvidenceImportService service = service(properties, histories, List.of());

        DisclosureEvidenceImportHistory history = service.importDisclosures(
                "005930", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(history.status()).isEqualTo(DisclosureEvidenceImportStatus.SKIPPED);
        assertThat(histories.values).containsExactly(history);
    }

    @Test
    void importsProviderMetadataAsEvidenceWithoutRawBodyStorage() {
        InMemoryHistoryPort histories = new InMemoryHistoryPort();
        DisclosureProviderProperties properties = new DisclosureProviderProperties();
        properties.setEnabled(true);
        CatalystEvidenceService evidenceService = new CatalystEvidenceService(
                new CatalystEvidenceServiceTest.InMemoryEvidencePort(),
                OperationalMetricsPort.noop(), Clock.fixed(NOW, ZoneOffset.UTC));
        DisclosureEvidenceImportService service = new DisclosureEvidenceImportService(
                (stockCode, fromDate, toDate) -> List.of(new DisclosureEvidenceRecord(
                        stockCode, CatalystEvidenceType.DART_DISCLOSURE, "공시 제목",
                        "공시 메타데이터 요약", "DART", "https://example.test/disclosure",
                        NOW, EvidenceConfidence.HIGH)),
                histories, evidenceService, properties, OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        DisclosureEvidenceImportHistory history = service.importDisclosures(
                "005930", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(history.status()).isEqualTo(DisclosureEvidenceImportStatus.SUCCESS);
        assertThat(history.importedCount()).isEqualTo(1);
        assertThat(evidenceService.findByStockCode("005930")).hasSize(1);
    }

    private static DisclosureEvidenceImportService service(
            DisclosureProviderProperties properties,
            InMemoryHistoryPort histories,
            List<DisclosureEvidenceRecord> records
    ) {
        CatalystEvidenceService evidenceService = new CatalystEvidenceService(
                new CatalystEvidenceServiceTest.InMemoryEvidencePort(),
                OperationalMetricsPort.noop(), Clock.fixed(NOW, ZoneOffset.UTC));
        return new DisclosureEvidenceImportService(
                (stockCode, fromDate, toDate) -> records,
                histories, evidenceService, properties, OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static class InMemoryHistoryPort implements DisclosureEvidenceImportHistoryPort {
        private final List<DisclosureEvidenceImportHistory> values = new ArrayList<>();
        @Override public DisclosureEvidenceImportHistory save(DisclosureEvidenceImportHistory value) {
            values.add(value);
            return value;
        }
        @Override public List<DisclosureEvidenceImportHistory> findRecentDisclosureImports(int limit) {
            return values.stream().limit(limit).toList();
        }
    }
}
