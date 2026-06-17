package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EarningsPreviewServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Test
    void generatesDraftFromThesisLatestAnalysisValuationIndicatorAndCatalyst() {
        InMemoryPreviewPort previews = new InMemoryPreviewPort();
        EarningsPreviewService service = new EarningsPreviewService(
                previews,
                eventPort(),
                thesisPort(),
                analysisPort(),
                valuationPort(),
                indicatorPort(),
                catalystPort(),
                OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        EarningsPreview result = service.generate("005930", 1L, LocalDate.of(2026, 7, 25));

        assertThat(result.status()).isEqualTo(EarningsPreviewStatus.DRAFT);
        assertThat(result.keyCheckpoints()).anyMatch(value -> value.contains("LATEST_EARNINGS_STATUS STRONG"));
        assertThat(result.keyCheckpoints()).anyMatch(value -> value.contains("VALUATION"));
        assertThat(result.keyCheckpoints()).anyMatch(value -> value.contains("TECHNICAL"));
        assertThat(result.keyCheckpoints()).anyMatch(value -> value.contains("UPCOMING_CATALYST"));
        assertThat(result.thesisWatchPoints()).anyMatch(value -> value.contains("HBM recovery"));
    }

    private static EarningsEventPort eventPort() {
        return new EarningsEventPort() {
            @Override public EarningsEvent save(EarningsEvent value) { return value; }
            @Override
            public Optional<EarningsEvent> findById(long id) {
                return Optional.of(new EarningsEvent(1L, "005930", 2026, 2,
                        LocalDate.of(2026, 7, 31), null, EarningsEventStatus.SCHEDULED,
                        null, NOW, NOW));
            }
            @Override public Optional<EarningsEvent> findEventByStockCodeAndQuarter(String stockCode, int fiscalYear, int fiscalQuarter) { return Optional.empty(); }
            @Override public List<EarningsEvent> find(String stockCode, LocalDate from, LocalDate to) { return List.of(); }
            @Override public List<EarningsEvent> findByStatusAndExpectedAnnouncementDateBetween(EarningsEventStatus status, LocalDate from, LocalDate to) { return List.of(); }
        };
    }

    private static InvestmentThesisPort thesisPort() {
        return new InvestmentThesisPort() {
            @Override public InvestmentThesis save(InvestmentThesis thesis) { return thesis; }
            @Override public Optional<InvestmentThesis> findThesisById(long id) { return Optional.empty(); }
            @Override
            public List<InvestmentThesis> find(String stockCode, ThesisStatus status) {
                return List.of(new InvestmentThesis(1L, stockCode, "HBM recovery",
                        "memory margin improves", "margin declines", null,
                        "close below MA60", 80, ThesisStatus.ACTIVE, NOW, NOW));
            }
        };
    }

    private static EarningsAnalysisPort analysisPort() {
        return new EarningsAnalysisPort() {
            @Override public EarningsAnalysisSnapshot save(EarningsAnalysisSnapshot value) { return value; }
            @Override public Optional<EarningsAnalysisSnapshot> findByStockCodeAndBaseDate(String stockCode, LocalDate baseDate) { return Optional.empty(); }
            @Override
            public Optional<EarningsAnalysisSnapshot> findLatestByStockCode(String stockCode) {
                return Optional.of(new EarningsAnalysisSnapshot(1L, stockCode, LocalDate.of(2026, 6, 15),
                        null, null, null, null, null, null, null, null,
                        null, null, null, 30, 35, 65, EarningsAnalysisStatus.STRONG,
                        List.of(), NOW, NOW));
            }
            @Override public List<EarningsAnalysisSnapshot> findByBaseDate(LocalDate baseDate) { return List.of(); }
        };
    }

    private static ValuationSnapshotPort valuationPort() {
        return new ValuationSnapshotPort() {
            @Override public ValuationSnapshot save(ValuationSnapshot value) { return value; }
            @Override
            public Optional<ValuationSnapshot> findLatestByStockCode(String stockCode, LocalDate baseDate) {
                return Optional.of(new ValuationSnapshot(1L, stockCode, baseDate,
                        new BigDecimal("1000"), new BigDecimal("12"),
                        new BigDecimal("1.2"), new BigDecimal("1.8"),
                        null, null, null, NOW, NOW));
            }
        };
    }

    private static IndicatorSnapshotPort indicatorPort() {
        return new IndicatorSnapshotPort() {
            @Override public IndicatorSnapshot save(IndicatorSnapshot snapshot) { return snapshot; }
            @Override
            public List<IndicatorSnapshot> findByStockCodeAndTradeDateBetween(String stockCode, LocalDate from, LocalDate to) {
                return List.of(new IndicatorSnapshot(stockCode, LocalDate.of(2026, 7, 24),
                        BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("20"),
                        new BigDecimal("50"), BigDecimal.ONE, BigDecimal.ONE,
                        BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO));
            }
        };
    }

    private static InvestmentCatalystPort catalystPort() {
        return new InvestmentCatalystPort() {
            @Override public InvestmentCatalyst save(InvestmentCatalyst catalyst) { return catalyst; }
            @Override public Optional<InvestmentCatalyst> findCatalystById(long id) { return Optional.empty(); }
            @Override
            public List<InvestmentCatalyst> find(String stockCode, LocalDate from, LocalDate to, CatalystStatus status) {
                return List.of(new InvestmentCatalyst(1L, stockCode, "2Q earnings",
                        CatalystType.EARNINGS, LocalDate.of(2026, 7, 31),
                        CatalystImportance.HIGH, CatalystStatus.UPCOMING, null, null, NOW, NOW));
            }
        };
    }

    private static class InMemoryPreviewPort implements EarningsPreviewPort {
        @Override public EarningsPreview save(EarningsPreview value) { return value; }
        @Override public Optional<EarningsPreview> findPreviewById(long id) { return Optional.empty(); }
        @Override public Optional<EarningsPreview> findLatestByEarningsEventId(long earningsEventId) { return Optional.empty(); }
        @Override public List<EarningsPreview> findPreviewsByStockCode(String stockCode) { return List.of(); }
        @Override public List<EarningsPreview> findByStatusAndPreviewDateBetween(EarningsPreviewStatus status, LocalDate from, LocalDate to) { return List.of(); }
    }
}
