package seokhoon.trade.application.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import seokhoon.trade.adapter.metrics.MicrometerOperationalMetricsAdapter;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpDecision;
import seokhoon.trade.application.port.out.EarlyMarketFollowUpResultPort;
import seokhoon.trade.application.port.out.EarlyMarketPerformancePort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.market.EarlyMarketCandidatePerformance;
import seokhoon.trade.domain.market.EarlyMarketFollowUpRecord;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EarlyMarketStrategyReportServiceTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);

    @Test
    void aggregatesReturnsMissingPerformanceBucketsAndBestWorstCandidates() {
        List<TradingSignalRecord> signals = List.of(
                signal(1L, SignalType.EARLY_MARKET_PRE_SCAN, 75, List.of()),
                signal(
                        2L,
                        SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                        85,
                        List.of("PREVIOUS_HIGH_NOT_BROKEN", "OPENING_PRICE_LOST")
                ),
                signal(
                        3L,
                        SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                        95,
                        List.of("PREVIOUS_HIGH_BROKEN", "OPENING_PRICE_HELD")
                )
        );
        EarlyMarketStrategyReportService service = service(
                signals,
                List.of(
                        performance(1L, SignalType.EARLY_MARKET_PRE_SCAN, "4", "-2", false),
                        performance(
                                3L,
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                                "10",
                                "-4",
                                true
                        )
                ),
                List.of(
                        followUp(2L, EarlyMarketFollowUpDecision.CAUTION),
                        followUp(3L, EarlyMarketFollowUpDecision.KEEP)
                )
        );

        var report = service.loadDailyReport(TRADE_DATE);

        assertThat(report.preScanCount()).isEqualTo(1);
        assertThat(report.entryCandidateCount()).isEqualTo(2);
        assertThat(report.performanceCapturedCount()).isEqualTo(2);
        assertThat(report.excludedFromPerformanceCount()).isEqualTo(1);
        assertThat(report.averageMaxReturnRate()).isEqualByComparingTo("7.0000");
        assertThat(report.averageMaxDrawdownRate()).isEqualByComparingTo("-3.0000");
        assertThat(report.bestCandidate().signalId()).isEqualTo(3L);
        assertThat(report.worstCandidate().signalId()).isEqualTo(1L);
        assertThat(report.byScoreBucket())
                .containsKeys("70-79", "80-89", "90+");
        assertThat(report.byScoreBucket().get("80-89").performanceCapturedCount())
                .isZero();
        assertThat(report.byVwapBroken().get("TRUE").candidateCount()).isEqualTo(1);
        assertThat(report.byVwapBroken().get("FALSE").candidateCount()).isEqualTo(1);
        assertThat(report.byVwapBroken().get("UNKNOWN").candidateCount()).isEqualTo(1);
        assertThat(report.byPreviousHighBreakout().get("TRUE").candidateCount())
                .isEqualTo(1);
        assertThat(report.byOpeningPriceHeld().get("FALSE").candidateCount())
                .isEqualTo(1);
        assertThat(report.byFollowUpDecision().get("KEEP").candidateCount())
                .isEqualTo(1);
        assertThat(report.byFollowUpDecision().get("KEEP").averageMaxReturnRate())
                .isEqualByComparingTo("10.0000");
        assertThat(report.byFollowUpDecision().get("CAUTION").candidateCount())
                .isEqualTo(1);
        assertThat(report.byFollowUpDecision().get("UNKNOWN").candidateCount())
                .isEqualTo(1);
        assertThat(report.candidates().get(2).followUpDecision())
                .isEqualTo(EarlyMarketFollowUpDecision.KEEP);
        assertThat(report.dataCompleteness().maxReturnSampleCount()).isEqualTo(2);
        assertThat(report.candidates()).hasSize(3);
    }

    @Test
    void excludesNullRatesFromAverageEvenWhenPerformanceWasCaptured() {
        EarlyMarketStrategyReportService service = service(
                List.of(signal(
                        1L,
                        SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                        90,
                        List.of()
                )),
                List.of(performance(
                        1L,
                        SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                        null,
                        null,
                        null
                )),
                List.of()
        );

        var report = service.loadDailyReport(TRADE_DATE);

        assertThat(report.performanceCapturedCount()).isEqualTo(1);
        assertThat(report.excludedFromPerformanceCount()).isZero();
        assertThat(report.averageMaxReturnRate()).isNull();
        assertThat(report.averageMaxDrawdownRate()).isNull();
        assertThat(report.bestCandidate()).isNull();
        assertThat(report.dataCompleteness().maxReturnSampleCount()).isZero();
    }

    @Test
    void recordsNoDataAndFailureMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EarlyMarketStrategyReportService noDataService = new EarlyMarketStrategyReportService(
                criteria -> List.of(),
                performancePort(List.of()),
                new MicrometerOperationalMetricsAdapter(registry)
        );

        noDataService.loadDailyReport(TRADE_DATE);

        assertThat(registry.find("tradeguard.early_market.report.count")
                .tag("result", "no_data")
                .counter().count()).isEqualTo(1.0);

        EarlyMarketStrategyReportService failingService =
                new EarlyMarketStrategyReportService(
                        criteria -> {
                            throw new IllegalStateException("signal lookup failed");
                        },
                        performancePort(List.of()),
                        new MicrometerOperationalMetricsAdapter(registry)
                );
        assertThatThrownBy(() -> failingService.loadDailyReport(TRADE_DATE))
                .isInstanceOf(IllegalStateException.class);
        assertThat(registry.find("tradeguard.early_market.report.count")
                .tag("result", "failure")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void aggregatesWeightedAveragesAndWinRateAcrossTradingDays() {
        LocalDate previousTradeDate = TRADE_DATE.minusDays(1);
        EarlyMarketStrategyReportService service = service(
                List.of(
                        signal(
                                1L,
                                previousTradeDate,
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                                80,
                                List.of()
                        ),
                        signal(
                                2L,
                                TRADE_DATE,
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                                90,
                                List.of()
                        ),
                        signal(
                                3L,
                                TRADE_DATE,
                                SignalType.EARLY_MARKET_PRE_SCAN,
                                75,
                                List.of()
                        )
                ),
                List.of(
                        performance(
                                1L,
                                previousTradeDate,
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                                "10",
                                "-2",
                                false
                        ),
                        performance(
                                2L,
                                TRADE_DATE,
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                                "-4",
                                "-6",
                                true
                        ),
                        performance(
                                3L,
                                TRADE_DATE,
                                SignalType.EARLY_MARKET_PRE_SCAN,
                                null,
                                "-1",
                                false
                        )
                ),
                List.of()
        );

        var report = service.loadPeriodReport(previousTradeDate, TRADE_DATE);

        assertThat(report.tradingDayCount()).isEqualTo(2);
        assertThat(report.candidateCount()).isEqualTo(3);
        assertThat(report.averageMaxReturnRate()).isEqualByComparingTo("3.0000");
        assertThat(report.averageMaxDrawdownRate()).isEqualByComparingTo("-3.0000");
        assertThat(report.winRate()).isEqualByComparingTo("50.0000");
        assertThat(report.dataCompleteness().winSampleCount()).isEqualTo(2);
        assertThat(report.dataCompleteness().winCount()).isEqualTo(1);
        assertThat(report.bestCandidate().tradeDate()).isEqualTo(previousTradeDate);
        assertThat(report.worstCandidate().tradeDate()).isEqualTo(TRADE_DATE);
        assertThat(report.byTradeDate()).containsOnlyKeys(
                previousTradeDate,
                TRADE_DATE
        );
    }

    @Test
    void aggregatesFollowUpDecisionsAcrossTradingDays() {
        LocalDate previousTradeDate = TRADE_DATE.minusDays(1);
        EarlyMarketStrategyReportService service = service(
                List.of(
                        signal(
                                1L,
                                previousTradeDate,
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                                80,
                                List.of()
                        ),
                        signal(
                                2L,
                                TRADE_DATE,
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                                90,
                                List.of()
                        ),
                        signal(
                                3L,
                                TRADE_DATE,
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                                95,
                                List.of()
                        )
                ),
                List.of(
                        performance(
                                1L,
                                previousTradeDate,
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                                "2",
                                "-1",
                                false
                        ),
                        performance(
                                2L,
                                TRADE_DATE,
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                                "4",
                                "-2",
                                false
                        )
                ),
                List.of(
                        followUp(
                                1L,
                                previousTradeDate,
                                EarlyMarketFollowUpDecision.KEEP
                        ),
                        followUp(
                                2L,
                                TRADE_DATE,
                                EarlyMarketFollowUpDecision.KEEP
                        ),
                        followUp(
                                3L,
                                TRADE_DATE,
                                EarlyMarketFollowUpDecision.EXCLUDE
                        )
                )
        );

        var report = service.loadPeriodReport(previousTradeDate, TRADE_DATE);

        assertThat(report.byFollowUpDecision().get("KEEP").candidateCount())
                .isEqualTo(2);
        assertThat(report.byFollowUpDecision().get("KEEP").averageMaxReturnRate())
                .isEqualByComparingTo("3.0000");
        assertThat(report.byFollowUpDecision().get("EXCLUDE").candidateCount())
                .isEqualTo(1);
        assertThat(report.byFollowUpDecision().get("CAUTION").candidateCount())
                .isZero();
    }

    @Test
    void rejectsInvalidPeriodRanges() {
        EarlyMarketStrategyReportService service = service(
                List.of(),
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> service.loadPeriodReport(
                TRADE_DATE,
                TRADE_DATE.minusDays(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from must be on or before to");
        assertThatThrownBy(() -> service.loadPeriodReport(
                TRADE_DATE.minusDays(90),
                TRADE_DATE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("period must not exceed 90 days");
    }

    @Test
    void returnsEmptyPeriodReportAndRecordsNoDataMetric() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EarlyMarketStrategyReportService service =
                new EarlyMarketStrategyReportService(
                        criteria -> List.of(),
                        performancePort(List.of()),
                        new MicrometerOperationalMetricsAdapter(registry)
                );

        var report = service.loadPeriodReport(
                TRADE_DATE.minusDays(2),
                TRADE_DATE
        );

        assertThat(report.tradingDayCount()).isEqualTo(3);
        assertThat(report.candidateCount()).isZero();
        assertThat(report.winRate()).isNull();
        assertThat(report.byTradeDate()).isEmpty();
        assertThat(registry.find("tradeguard.early_market.period_report.count")
                .tag("result", "no_data")
                .counter().count()).isEqualTo(1.0);
    }

    private static EarlyMarketStrategyReportService service(
            List<TradingSignalRecord> signals,
            List<EarlyMarketCandidatePerformance> performances,
            List<EarlyMarketFollowUpRecord> followUps
    ) {
        return new EarlyMarketStrategyReportService(
                (TradingSignalSearchCriteria criteria) -> signals.stream()
                        .filter(signal -> signal.signalDate()
                                .equals(criteria.signalDate()))
                        .filter(signal -> signal.signalType() == criteria.signalType())
                        .toList(),
                performancePort(performances),
                followUpPort(followUps),
                seokhoon.trade.application.port.out.OperationalMetricsPort.noop()
        );
    }

    private static EarlyMarketFollowUpResultPort followUpPort(
            List<EarlyMarketFollowUpRecord> followUps
    ) {
        return new EarlyMarketFollowUpResultPort() {
            @Override
            public EarlyMarketFollowUpRecord save(EarlyMarketFollowUpRecord result) {
                return result;
            }

            @Override
            public List<EarlyMarketFollowUpRecord> findByTradeDate(
                    LocalDate tradeDate
            ) {
                return followUps.stream()
                        .filter(result -> result.tradeDate().equals(tradeDate))
                        .toList();
            }

            @Override
            public Optional<EarlyMarketFollowUpRecord> findBySignalId(long signalId) {
                return followUps.stream()
                        .filter(result -> result.signalId() == signalId)
                        .findFirst();
            }
        };
    }

    private static EarlyMarketPerformancePort performancePort(
            List<EarlyMarketCandidatePerformance> performances
    ) {
        return new EarlyMarketPerformancePort() {
            @Override
            public EarlyMarketCandidatePerformance save(
                    EarlyMarketCandidatePerformance performance
            ) {
                return performance;
            }

            @Override
            public List<EarlyMarketCandidatePerformance> findByTradeDate(
                    LocalDate tradeDate
            ) {
                return performances.stream()
                        .filter(performance -> performance.tradeDate().equals(tradeDate))
                        .toList();
            }

            @Override
            public Optional<EarlyMarketCandidatePerformance> findBySignalId(
                    long signalId
            ) {
                return performances.stream()
                        .filter(performance -> performance.signalId() == signalId)
                        .findFirst();
            }
        };
    }

    private static TradingSignalRecord signal(
            long id,
            SignalType signalType,
            int score,
            List<String> reasons
    ) {
        return signal(id, TRADE_DATE, signalType, score, reasons);
    }

    private static TradingSignalRecord signal(
            long id,
            LocalDate tradeDate,
            SignalType signalType,
            int score,
            List<String> reasons
    ) {
        return new TradingSignalRecord(
                id,
                EarlyMarketPreOpenScanner.STRATEGY_NAME,
                "STOCK" + id,
                tradeDate,
                signalType,
                score,
                reasons,
                List.of(),
                TradingSignalStatus.CREATED
        );
    }

    private static EarlyMarketCandidatePerformance performance(
            long signalId,
            SignalType signalType,
            String maxReturn,
            String maxDrawdown,
            Boolean vwapBroken
    ) {
        return performance(
                signalId,
                TRADE_DATE,
                signalType,
                maxReturn,
                maxDrawdown,
                vwapBroken
        );
    }

    private static EarlyMarketCandidatePerformance performance(
            long signalId,
            LocalDate tradeDate,
            SignalType signalType,
            String maxReturn,
            String maxDrawdown,
            Boolean vwapBroken
    ) {
        return new EarlyMarketCandidatePerformance(
                signalId,
                "STOCK" + signalId,
                tradeDate,
                signalType,
                null,
                null,
                null,
                BigDecimal.valueOf(100),
                maxReturn == null ? null : new BigDecimal(maxReturn),
                maxDrawdown == null ? null : new BigDecimal(maxDrawdown),
                vwapBroken,
                Instant.parse("2026-06-10T00:31:00Z")
        );
    }

    private static EarlyMarketFollowUpRecord followUp(
            long signalId,
            EarlyMarketFollowUpDecision decision
    ) {
        return followUp(signalId, TRADE_DATE, decision);
    }

    private static EarlyMarketFollowUpRecord followUp(
            long signalId,
            LocalDate tradeDate,
            EarlyMarketFollowUpDecision decision
    ) {
        return new EarlyMarketFollowUpRecord(
                signalId,
                tradeDate,
                "STOCK" + signalId,
                decision,
                90,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(105),
                new BigDecimal("-1.0"),
                false,
                List.of(decision.name()),
                Instant.parse("2026-06-10T00:20:00Z")
        );
    }
}
