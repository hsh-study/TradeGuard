package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.EarlyMarketPerformanceCaptureResult;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.EarlyMarketPerformancePort;
import seokhoon.trade.application.port.out.IntradayMarketSnapshot;
import seokhoon.trade.application.port.out.MarketSnapshotPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.market.EarlyMarketCandidatePerformance;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyMarketPerformanceServiceTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);
    private static final Instant CAPTURED_AT = Instant.parse("2026-06-10T00:31:00Z");

    @Test
    void capturesPreScanAndEntryCandidatePerformancesWithSignalScores() {
        SignalQuery signals = new SignalQuery(List.of(
                signal(1L, SignalType.EARLY_MARKET_PRE_SCAN, 80, "005930"),
                signal(2L, SignalType.EARLY_MARKET_ENTRY_CANDIDATE, 105, "000660")
        ));
        SnapshotQuery snapshots = new SnapshotQuery(Map.of(
                "005930", snapshot("005930", "76000", "75000"),
                "000660", snapshot("000660", "180000", "181000")
        ));
        PerformanceStore performances = new PerformanceStore();

        EarlyMarketPerformanceCaptureResult result =
                service(signals, snapshots, performances).capture(TRADE_DATE);

        assertThat(result.signalCount()).isEqualTo(2);
        assertThat(result.capturedCount()).isEqualTo(2);
        assertThat(result.performances())
                .extracting(
                        performance -> performance.signalType(),
                        performance -> performance.signalScore()
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                SignalType.EARLY_MARKET_PRE_SCAN,
                                80
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                                105
                        )
                );
        assertThat(result.performances().get(0).priceAt0930())
                .isEqualByComparingTo("76000");
        assertThat(result.performances().get(0).vwapBroken()).isFalse();
        assertThat(result.performances().get(1).vwapBroken()).isTrue();
    }

    @Test
    void storesNullableFieldsWhenSnapshotIsUnavailable() {
        SignalQuery signals = new SignalQuery(List.of(
                signal(1L, SignalType.EARLY_MARKET_PRE_SCAN, 80, "005930")
        ));
        PerformanceStore performances = new PerformanceStore();

        EarlyMarketPerformanceCaptureResult result = service(
                signals,
                stockCode -> Optional.empty(),
                performances
        ).capture(TRADE_DATE);

        assertThat(result.capturedCount()).isEqualTo(1);
        assertThat(result.performances()).singleElement().satisfies(performance -> {
            assertThat(performance.entryReferencePrice()).isNull();
            assertThat(performance.highUntil0930()).isNull();
            assertThat(performance.lowUntil0930()).isNull();
            assertThat(performance.priceAt0930()).isNull();
            assertThat(performance.maxReturnRateUntil0930()).isNull();
            assertThat(performance.maxDrawdownRateUntil0930()).isNull();
            assertThat(performance.vwapBroken()).isNull();
        });
    }

    @Test
    void loadsSavedPerformanceByTradeDateAndSignalId() {
        SignalQuery signals = new SignalQuery(List.of(
                signal(1L, SignalType.EARLY_MARKET_PRE_SCAN, 87, "005930")
        ));
        PerformanceStore performances = new PerformanceStore();
        performances.save(new EarlyMarketCandidatePerformance(
                1L,
                "005930",
                TRADE_DATE,
                SignalType.EARLY_MARKET_PRE_SCAN,
                null,
                null,
                null,
                new BigDecimal("76500"),
                null,
                null,
                false,
                CAPTURED_AT
        ));
        EarlyMarketPerformanceService service =
                service(signals, stockCode -> Optional.empty(), performances);

        assertThat(service.findByTradeDate(TRADE_DATE))
                .singleElement()
                .satisfies(performance -> {
                    assertThat(performance.signalId()).isEqualTo(1L);
                    assertThat(performance.signalScore()).isEqualTo(87);
                });
        assertThat(service.findBySignalId(1L).priceAt0930())
                .isEqualByComparingTo("76500");
    }

    private static EarlyMarketPerformanceService service(
            TradingSignalQueryPort signals,
            MarketSnapshotPort snapshots,
            EarlyMarketPerformancePort performances
    ) {
        return new EarlyMarketPerformanceService(
                signals,
                snapshots,
                performances,
                OperationalMetricsPort.noop(),
                Clock.fixed(CAPTURED_AT, ZoneOffset.UTC)
        );
    }

    private static TradingSignalRecord signal(
            long id,
            SignalType signalType,
            int score,
            String stockCode
    ) {
        return new TradingSignalRecord(
                id,
                EarlyMarketPreOpenScanner.STRATEGY_NAME,
                stockCode,
                TRADE_DATE,
                signalType,
                score,
                List.of(),
                List.of(),
                TradingSignalStatus.CREATED
        );
    }

    private static IntradayMarketSnapshot snapshot(
            String stockCode,
            String currentPrice,
            String vwap
    ) {
        return new IntradayMarketSnapshot(
                stockCode,
                new BigDecimal(currentPrice),
                BigDecimal.valueOf(3),
                new BigDecimal(currentPrice),
                new BigDecimal(currentPrice),
                1_000_000,
                BigDecimal.valueOf(50_000_000_000L),
                new BigDecimal(vwap),
                CAPTURED_AT
        );
    }

    private record SignalQuery(List<TradingSignalRecord> signals)
            implements TradingSignalQueryPort {
        @Override
        public List<TradingSignalRecord> find(TradingSignalSearchCriteria criteria) {
            return signals.stream()
                    .filter(signal -> signal.signalDate().equals(criteria.signalDate()))
                    .filter(signal -> signal.signalType() == criteria.signalType())
                    .toList();
        }
    }

    private record SnapshotQuery(Map<String, IntradayMarketSnapshot> snapshots)
            implements MarketSnapshotPort {
        @Override
        public Optional<IntradayMarketSnapshot> getSnapshot(String stockCode) {
            return Optional.ofNullable(snapshots.get(stockCode));
        }
    }

    private static class PerformanceStore implements EarlyMarketPerformancePort {
        private final Map<Long, EarlyMarketCandidatePerformance> values =
                new LinkedHashMap<>();

        @Override
        public EarlyMarketCandidatePerformance save(
                EarlyMarketCandidatePerformance performance
        ) {
            values.put(performance.signalId(), performance);
            return performance;
        }

        @Override
        public List<EarlyMarketCandidatePerformance> findByTradeDate(
                LocalDate tradeDate
        ) {
            return new ArrayList<>(values.values()).stream()
                    .filter(performance -> performance.tradeDate().equals(tradeDate))
                    .toList();
        }

        @Override
        public Optional<EarlyMarketCandidatePerformance> findBySignalId(long signalId) {
            return Optional.ofNullable(values.get(signalId));
        }
    }
}
