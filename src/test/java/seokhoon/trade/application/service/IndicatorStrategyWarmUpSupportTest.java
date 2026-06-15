package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.WarmUpDailyPricesAndIndicatorsUseCase;
import seokhoon.trade.application.port.out.IndicatorSnapshotPort;
import seokhoon.trade.config.IndicatorWarmUpProperties;
import seokhoon.trade.domain.indicator.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class IndicatorStrategyWarmUpSupportTest {
    @Test
    void runsWarmupBeforeAssessmentAndExcludesInStrictMode() {
        AtomicBoolean called = new AtomicBoolean();
        IndicatorWarmUpProperties properties =
                new IndicatorWarmUpProperties();
        properties.setFailStrategyWhenInsufficient(true);
        WarmUpDailyPricesAndIndicatorsUseCase warmUp =
                new WarmUpDailyPricesAndIndicatorsUseCase() {
                    @Override
                    public IndicatorWarmUpResult warmUpStock(
                            String stockCode, LocalDate baseDate) {
                        called.set(true);
                        return insufficient(stockCode, baseDate);
                    }

                    @Override
                    public List<IndicatorWarmUpResult> warmUpStocks(
                            List<String> stockCodes, LocalDate baseDate) {
                        called.set(true);
                        return stockCodes.stream()
                                .map(code -> insufficient(code, baseDate))
                                .toList();
                    }
                };
        IndicatorSnapshotPort snapshots = new IndicatorSnapshotPort() {
            @Override
            public IndicatorSnapshot save(IndicatorSnapshot snapshot) {
                return snapshot;
            }

            @Override
            public List<IndicatorSnapshot>
            findByStockCodeAndTradeDateBetween(
                    String stockCode, LocalDate from, LocalDate to) {
                return List.of();
            }
        };
        IndicatorStrategyWarmUpSupport support =
                new IndicatorStrategyWarmUpSupport(
                        warmUp, snapshots, properties);

        IndicatorStrategyWarmUpSupport.Session session =
                support.prepare(List.of("005930"),
                        LocalDate.of(2026, 6, 15));
        IndicatorStrategyWarmUpSupport.Assessment assessment =
                session.assess("005930", BigDecimal.valueOf(70_000));

        assertThat(called).isTrue();
        assertThat(assessment.excluded()).isTrue();
        assertThat(assessment.reasons())
                .contains("INDICATOR_DATA_INSUFFICIENT");
    }

    @Test
    void continuesStrategyWithWarningWhenStrictModeIsDisabled() {
        IndicatorWarmUpProperties properties =
                new IndicatorWarmUpProperties();
        IndicatorStrategyWarmUpSupport support =
                new IndicatorStrategyWarmUpSupport(
                        new PartialWarmUp(),
                        emptySnapshots(),
                        properties
                );

        IndicatorStrategyWarmUpSupport.Assessment assessment =
                support.prepare(List.of("005930"),
                                LocalDate.of(2026, 6, 15))
                        .assess("005930", BigDecimal.valueOf(70_000));

        assertThat(assessment.excluded()).isFalse();
        assertThat(assessment.reasons())
                .contains("INDICATOR_DATA_INSUFFICIENT");
    }

    private static IndicatorWarmUpResult insufficient(
            String stockCode,
            LocalDate baseDate
    ) {
        return new IndicatorWarmUpResult(stockCode, baseDate,
                baseDate.minusMonths(4), baseDate.minusDays(1),
                0, 30, false, true, false,
                List.of("INDICATOR_DATA_INSUFFICIENT"),
                IndicatorWarmUpStatus.PARTIAL);
    }

    private static IndicatorSnapshotPort emptySnapshots() {
        return new IndicatorSnapshotPort() {
            @Override
            public IndicatorSnapshot save(IndicatorSnapshot snapshot) {
                return snapshot;
            }

            @Override
            public List<IndicatorSnapshot>
            findByStockCodeAndTradeDateBetween(
                    String stockCode, LocalDate from, LocalDate to) {
                return List.of();
            }
        };
    }

    private static class PartialWarmUp
            implements WarmUpDailyPricesAndIndicatorsUseCase {
        @Override
        public IndicatorWarmUpResult warmUpStock(
                String stockCode, LocalDate baseDate) {
            return insufficient(stockCode, baseDate);
        }

        @Override
        public List<IndicatorWarmUpResult> warmUpStocks(
                List<String> stockCodes, LocalDate baseDate) {
            return stockCodes.stream()
                    .map(code -> insufficient(code, baseDate))
                    .toList();
        }
    }
}
