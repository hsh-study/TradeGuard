package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.EarningsAnalysisPort;
import seokhoon.trade.config.EarningsStrategyProperties;
import seokhoon.trade.domain.research.EarningsAnalysisSnapshot;
import seokhoon.trade.domain.research.EarningsAnalysisStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EarningsStrategyAdjustmentTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Test
    void addsScoreForStrongEarnings() {
        EarningsStrategyProperties properties = new EarningsStrategyProperties();
        EarningsStrategyAdjustment adjustment = new EarningsStrategyAdjustment(
                port(snapshot(EarningsAnalysisStatus.STRONG, 65)), properties);

        EarningsStrategyAdjustment.Assessment result = adjustment.assess("005930");

        assertThat(result.scoreAdjustment()).isEqualTo(10);
        assertThat(result.excluded()).isFalse();
        assertThat(result.reasons()).contains("EARNINGS_STATUS_STRONG");
    }

    @Test
    void canExcludeWeakEarnings() {
        EarningsStrategyProperties properties = new EarningsStrategyProperties();
        properties.setExcludeWeak(true);
        EarningsStrategyAdjustment adjustment = new EarningsStrategyAdjustment(
                port(snapshot(EarningsAnalysisStatus.WEAK, 5)), properties);

        EarningsStrategyAdjustment.Assessment result = adjustment.assess("005930");

        assertThat(result.scoreAdjustment()).isEqualTo(-10);
        assertThat(result.excluded()).isTrue();
        assertThat(result.reasons()).contains("EARNINGS_STATUS_WEAK");
    }

    private static EarningsAnalysisPort port(EarningsAnalysisSnapshot snapshot) {
        return new EarningsAnalysisPort() {
            @Override
            public EarningsAnalysisSnapshot save(EarningsAnalysisSnapshot value) {
                return value;
            }

            @Override
            public Optional<EarningsAnalysisSnapshot> findByStockCodeAndBaseDate(
                    String stockCode,
                    LocalDate baseDate
            ) {
                return Optional.of(snapshot);
            }

            @Override
            public Optional<EarningsAnalysisSnapshot> findLatestByStockCode(String stockCode) {
                return Optional.of(snapshot);
            }

            @Override
            public List<EarningsAnalysisSnapshot> findByBaseDate(LocalDate baseDate) {
                return List.of(snapshot);
            }
        };
    }

    private static EarningsAnalysisSnapshot snapshot(EarningsAnalysisStatus status, int score) {
        return new EarningsAnalysisSnapshot(1L, "005930", LocalDate.of(2026, 6, 15),
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, score, status, List.of(), NOW, NOW);
    }
}
