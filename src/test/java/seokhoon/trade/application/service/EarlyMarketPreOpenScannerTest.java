package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.EarlyMarketScanResult;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.IndicatorSnapshotPort;
import seokhoon.trade.application.port.out.MarketRankingPort;
import seokhoon.trade.application.port.out.MarketRankingStock;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
