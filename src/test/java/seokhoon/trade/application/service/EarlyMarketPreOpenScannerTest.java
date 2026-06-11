package seokhoon.trade.application.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import seokhoon.trade.adapter.metrics.MicrometerOperationalMetricsAdapter;
import seokhoon.trade.application.port.in.EarlyMarketScanResult;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.AfterHoursMarketDataPort;
import seokhoon.trade.application.port.out.IndicatorSnapshotPort;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.port.out.MarketRankingPort;
import seokhoon.trade.application.port.out.MarketRankingStock;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.market.AfterHoursQuote;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.config.EarlyMarketStrategyProperties;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyMarketPreOpenScannerTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);

    @Test
    void scoresRankingSourcesAndMovingAverageCondition() {
        SignalStore store = new SignalStore();
        MarketRankingStock stock = stock("005930", "5.0", "60000000000");
        EarlyMarketPreOpenScanner scanner = scanner(
                new RankingPort(List.of(stock), List.of(stock), List.of(stock)),
                indicatorPort(indicator("005930", "49000", "48000")),
                store,
                message -> NotificationDeliveryResult.success()
        );

        EarlyMarketScanResult result = scanner.scan(TRADE_DATE, 10);

        assertThat(result.scannedCount()).isEqualTo(1);
        assertThat(result.selectedCount()).isEqualTo(1);
        assertThat(store.saved).singleElement().satisfies(signal -> {
            assertThat(signal.strategyName()).isEqualTo("EARLY_MARKET_BREAKOUT");
            assertThat(signal.signalType()).isEqualTo(SignalType.EARLY_MARKET_PRE_SCAN);
            assertThat(signal.score()).isEqualTo(95);
            assertThat(signal.reasons()).contains(
                    "TRADING_VALUE_TOP",
                    "CHANGE_RATE_FAVORABLE",
                    "VOLUME_TOP",
                    "ABOVE_MA5_AND_MA20"
            );
        });
    }

    @Test
    void recordsOverheatAndMissingIndicatorWithoutForcingExtraScore() {
        SignalStore store = new SignalStore();
        MarketRankingStock stock = stock("005930", "17.0", "60000000000");
        EarlyMarketPreOpenScanner scanner = scanner(
                new RankingPort(List.of(stock), List.of(stock), List.of()),
                indicatorPort(),
                store,
                message -> NotificationDeliveryResult.skipped("disabled")
        );

        EarlyMarketScanResult result = scanner.scan(TRADE_DATE, 10);

        assertThat(result.briefingSent()).isFalse();
        assertThat(store.saved).singleElement().satisfies(signal -> {
            assertThat(signal.score()).isEqualTo(50);
            assertThat(signal.reasons())
                    .contains("OVERHEATED_CHANGE_RATE", "INDICATOR_DATA_UNAVAILABLE");
        });
    }

    @Test
    void limitsPreOpenCandidatesToTen() {
        SignalStore store = new SignalStore();
        List<MarketRankingStock> stocks = java.util.stream.IntStream.range(0, 12)
                .mapToObj(index -> stock("0000" + index, "5.0", "60000000000"))
                .toList();
        EarlyMarketPreOpenScanner scanner = scanner(
                new RankingPort(stocks, stocks, stocks),
                indicatorPort(),
                store,
                message -> NotificationDeliveryResult.success()
        );

        EarlyMarketScanResult result = scanner.scan(TRADE_DATE, 10);

        assertThat(result.selectedCount()).isEqualTo(10);
        assertThat(store.saved).hasSize(10);
    }

    @Test
    void addsAfterHoursStrengthAndTradingValueScores() {
        TradingSignal signal = scanWithAfterHours(
                Optional.of(afterHoursQuote("3.5", "30000000000"))
        );

        assertThat(signal.score()).isEqualTo(110);
        assertThat(signal.reasons()).contains(
                "AFTER_HOURS_CHANGE_RATE_OVER_3PCT",
                "AFTER_HOURS_TRADING_VALUE_SUFFICIENT",
                "AFTER_HOURS_SUMMARY_CHANGE_RATE_3.5_TRADING_VALUE_30000000000"
        );
    }

    @Test
    void appliesAfterHoursOverheatPenalty() {
        TradingSignal signal = scanWithAfterHours(
                Optional.of(afterHoursQuote("7.5", "30000000000"))
        );

        assertThat(signal.score()).isEqualTo(100);
        assertThat(signal.reasons()).contains("AFTER_HOURS_OVERHEATED");
    }

    @Test
    void appliesAfterHoursDeclinePenalty() {
        TradingSignal signal = scanWithAfterHours(
                Optional.of(afterHoursQuote("-3.0", "10000000000"))
        );

        assertThat(signal.score()).isEqualTo(70);
        assertThat(signal.reasons()).contains("AFTER_HOURS_DECLINE");
    }

    @Test
    void changingAfterHoursRiseThresholdChangesScore() {
        SignalStore store = new SignalStore();
        MarketRankingStock stock = stock("005930", "5.0", "60000000000");
        AfterHoursQuote quote = afterHoursQuote("3.5", "30000000000");
        AfterHoursMarketDataPort afterHoursPort = afterHoursPort(Optional.of(quote));
        EarlyMarketStrategyProperties properties = new EarlyMarketStrategyProperties();
        properties.getPreOpen().setAfterHoursRiseThreshold(new BigDecimal("4.0"));
        EarlyMarketPreOpenScanner scanner = new EarlyMarketPreOpenScanner(
                new RankingPort(List.of(stock), List.of(stock), List.of(stock)),
                indicatorPort(),
                afterHoursPort,
                date -> true,
                store,
                store,
                message -> NotificationDeliveryResult.success(),
                OperationalMetricsPort.noop(),
                properties,
                Clock.fixed(Instant.parse("2026-06-09T23:30:00Z"), ZoneOffset.UTC)
        );

        scanner.scan(TRADE_DATE, 10);

        assertThat(store.saved).singleElement().satisfies(signal -> {
            assertThat(signal.score()).isEqualTo(95);
            assertThat(signal.reasons())
                    .doesNotContain("AFTER_HOURS_CHANGE_RATE_OVER_3PCT");
        });
    }

    @Test
    void recordsReasonWithoutPenaltyWhenAfterHoursDataIsUnavailable() {
        TradingSignal signal = scanWithAfterHours(Optional.empty());

        assertThat(signal.score()).isEqualTo(80);
        assertThat(signal.reasons()).contains("AFTER_HOURS_DATA_UNAVAILABLE");
    }

    @Test
    void recordsFailureMetricWhenAfterHoursLookupThrows() {
        SignalStore store = new SignalStore();
        MarketRankingStock stock = stock("005930", "5.0", "60000000000");
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AfterHoursMarketDataPort failingPort = new AfterHoursMarketDataPort() {
            @Override
            public List<AfterHoursQuote> findTopAfterHoursMovers(
                    LocalDate tradeDate,
                    int limit
            ) {
                return List.of();
            }

            @Override
            public Optional<AfterHoursQuote> findByStockCode(
                    String stockCode,
                    LocalDate tradeDate
            ) {
                throw new IllegalStateException("KIS unavailable");
            }
        };
        EarlyMarketPreOpenScanner scanner = new EarlyMarketPreOpenScanner(
                new RankingPort(List.of(stock), List.of(stock), List.of(stock)),
                indicatorPort(),
                failingPort,
                store,
                store,
                message -> NotificationDeliveryResult.success(),
                new MicrometerOperationalMetricsAdapter(registry),
                Clock.fixed(
                        Instant.parse("2026-06-09T23:30:00Z"),
                        ZoneOffset.UTC
                )
        );

        scanner.scan(TRADE_DATE, 10);

        assertThat(registry.find("tradeguard.after_hours.lookup.count")
                .tag("result", "failure")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .noneMatch(tag -> tag.getValue().contains("005930"));
    }

    @Test
    void usesPreviousTradingDayForAfterHoursLookup() {
        SignalStore store = new SignalStore();
        MarketRankingStock stock = stock("005930", "5.0", "60000000000");
        AtomicReference<LocalDate> requestedDate = new AtomicReference<>();
        AfterHoursMarketDataPort afterHoursPort = new AfterHoursMarketDataPort() {
            @Override
            public List<AfterHoursQuote> findTopAfterHoursMovers(
                    LocalDate tradeDate,
                    int limit
            ) {
                return List.of();
            }

            @Override
            public Optional<AfterHoursQuote> findByStockCode(
                    String stockCode,
                    LocalDate tradeDate
            ) {
                requestedDate.set(tradeDate);
                return Optional.empty();
            }
        };
        MarketCalendarPort calendar = date ->
                date.getDayOfWeek() != java.time.DayOfWeek.SATURDAY
                        && date.getDayOfWeek() != java.time.DayOfWeek.SUNDAY
                        && !date.equals(LocalDate.of(2026, 2, 16))
                        && !date.equals(LocalDate.of(2026, 2, 17))
                        && !date.equals(LocalDate.of(2026, 2, 18));
        EarlyMarketPreOpenScanner scanner = new EarlyMarketPreOpenScanner(
                new RankingPort(List.of(stock), List.of(stock), List.of(stock)),
                indicatorPort(),
                afterHoursPort,
                calendar,
                store,
                store,
                message -> NotificationDeliveryResult.success(),
                OperationalMetricsPort.noop(),
                Clock.fixed(
                        Instant.parse("2026-02-18T23:30:00Z"),
                        ZoneOffset.UTC
                )
        );

        scanner.scan(LocalDate.of(2026, 2, 19), 10);

        assertThat(requestedDate.get()).isEqualTo(LocalDate.of(2026, 2, 13));
        assertThat(store.saved).singleElement().satisfies(signal ->
                assertThat(signal.reasons())
                        .contains("AFTER_HOURS_TRADE_DATE_2026-02-13")
        );
    }

    private static TradingSignal scanWithAfterHours(Optional<AfterHoursQuote> quote) {
        SignalStore store = new SignalStore();
        MarketRankingStock stock = stock("005930", "5.0", "60000000000");
        AfterHoursMarketDataPort afterHoursPort = afterHoursPort(quote);
        EarlyMarketPreOpenScanner scanner = new EarlyMarketPreOpenScanner(
                new RankingPort(List.of(stock), List.of(stock), List.of(stock)),
                indicatorPort(),
                afterHoursPort,
                store,
                store,
                message -> NotificationDeliveryResult.success(),
                OperationalMetricsPort.noop(),
                Clock.fixed(Instant.parse("2026-06-09T23:30:00Z"), ZoneOffset.UTC)
        );

        scanner.scan(TRADE_DATE, 10);
        return store.saved.getFirst();
    }

    private static AfterHoursMarketDataPort afterHoursPort(
            Optional<AfterHoursQuote> quote
    ) {
        return new AfterHoursMarketDataPort() {
            @Override
            public List<AfterHoursQuote> findTopAfterHoursMovers(
                    LocalDate tradeDate,
                    int limit
            ) {
                return quote.stream().toList();
            }

            @Override
            public Optional<AfterHoursQuote> findByStockCode(
                    String stockCode,
                    LocalDate tradeDate
            ) {
                return quote;
            }
        };
    }

    private static AfterHoursQuote afterHoursQuote(
            String changeRate,
            String tradingValue
    ) {
        return new AfterHoursQuote(
                "005930",
                "삼성전자",
                TRADE_DATE.minusDays(1),
                BigDecimal.valueOf(52_000),
                new BigDecimal(changeRate),
                100_000,
                new BigDecimal(tradingValue),
                Instant.parse("2026-06-09T09:30:00Z")
        );
    }

    private static EarlyMarketPreOpenScanner scanner(
            MarketRankingPort rankingPort,
            IndicatorSnapshotPort indicatorPort,
            SignalStore store,
            NotificationPort notificationPort
    ) {
        return new EarlyMarketPreOpenScanner(
                rankingPort,
                indicatorPort,
                store,
                store,
                notificationPort,
                Clock.fixed(Instant.parse("2026-06-09T23:30:00Z"), ZoneOffset.UTC)
        );
    }

    private static IndicatorSnapshotPort indicatorPort(IndicatorSnapshot... snapshots) {
        List<IndicatorSnapshot> values = List.of(snapshots);
        return new IndicatorSnapshotPort() {
            @Override
            public IndicatorSnapshot save(IndicatorSnapshot snapshot) {
                return snapshot;
            }

            @Override
            public List<IndicatorSnapshot> findByStockCodeAndTradeDateBetween(
                    String stockCode,
                    LocalDate from,
                    LocalDate to
            ) {
                return values.stream()
                        .filter(snapshot -> snapshot.stockCode().equals(stockCode))
                        .toList();
            }
        };
    }

    private static IndicatorSnapshot indicator(String stockCode, String ma5, String ma20) {
        return new IndicatorSnapshot(
                stockCode,
                TRADE_DATE.minusDays(1),
                new BigDecimal(ma5),
                new BigDecimal(ma20),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    private static MarketRankingStock stock(
            String stockCode,
            String changeRate,
            String tradingValue
    ) {
        return new MarketRankingStock(
                stockCode,
                "stock-" + stockCode,
                Market.KOSPI,
                BigDecimal.valueOf(50_000),
                new BigDecimal(changeRate),
                new BigDecimal(tradingValue),
                1_000_000
        );
    }

    private record RankingPort(
            List<MarketRankingStock> tradingValue,
            List<MarketRankingStock> rising,
            List<MarketRankingStock> volume
    ) implements MarketRankingPort {
        @Override
        public List<MarketRankingStock> findTopTradingValueStocks(Market market, int limit) {
            return tradingValue.stream().limit(limit).toList();
        }

        @Override
        public List<MarketRankingStock> findTopRisingStocks(Market market, int limit) {
            return rising.stream().limit(limit).toList();
        }

        @Override
        public List<MarketRankingStock> findVolumeSurgeStocks(Market market, int limit) {
            return volume.stream().limit(limit).toList();
        }
    }

    private static class SignalStore implements TradingSignalPort, TradingSignalQueryPort {
        private final List<TradingSignal> saved = new ArrayList<>();

        @Override
        public TradingSignal save(TradingSignal signal) {
            saved.removeIf(existing ->
                    existing.stockCode().equals(signal.stockCode())
                            && existing.signalType() == signal.signalType());
            saved.add(signal);
            return signal;
        }

        @Override
        public Optional<TradingSignal> find(
                String strategyName,
                String stockCode,
                LocalDate signalDate,
                SignalType signalType
        ) {
            return Optional.empty();
        }

        @Override
        public Optional<TradingSignal> findById(long signalId) {
            return Optional.empty();
        }

        @Override
        public List<TradingSignalRecord> find(TradingSignalSearchCriteria criteria) {
            List<TradingSignalRecord> records = new ArrayList<>();
            for (int index = 0; index < saved.size(); index++) {
                TradingSignal signal = saved.get(index);
                if (signal.signalType() == criteria.signalType()) {
                    records.add(new TradingSignalRecord(
                            (long) index + 1,
                            signal.strategyName(),
                            signal.stockCode(),
                            signal.signalDate(),
                            signal.signalType(),
                            signal.score(),
                            signal.reasons(),
                            signal.riskReasons(),
                            signal.status()
                    ));
                }
            }
            return records;
        }
    }
}
