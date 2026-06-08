package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ClosingBetCandidateScanResult;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.MarketRankingPort;
import seokhoon.trade.application.port.out.MarketRankingStock;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClosingBetCandidateScannerTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 5);

    @Test
    void removesDuplicateStocksAcrossRankingSources() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        ClosingBetCandidateScanner scanner = scanner(
                new FakeRankingPort(
                        List.of(stock("005930", "5.0", "60000000000", 1_000_000)),
                        List.of(stock("005930", "5.0", "60000000000", 1_000_000)),
                        List.of(stock("005930", "5.0", "60000000000", 1_000_000))
                ),
                signalStore,
                new RecordingNotificationPort()
        );

        ClosingBetCandidateScanResult result = scanner.scan(TRADE_DATE, 5);

        assertThat(result.scannedCount()).isEqualTo(1);
        assertThat(result.selectedCount()).isEqualTo(1);
        assertThat(signalStore.saved).hasSize(1);
    }

    @Test
    void excludesCandidatesBelowScoreSeventy() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        ClosingBetCandidateScanner scanner = scanner(
                new FakeRankingPort(
                        List.of(stock("005930", "3.1", "31000000000", 1_000_000)),
                        List.of(),
                        List.of()
                ),
                signalStore,
                new RecordingNotificationPort()
        );

        ClosingBetCandidateScanResult result = scanner.scan(TRADE_DATE, 5);

        assertThat(result.selectedCount()).isZero();
        assertThat(signalStore.saved).isEmpty();
    }

    @Test
    void selectsOnlyTopFiveCandidates() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        List<MarketRankingStock> stocks = java.util.stream.IntStream.range(0, 6)
                .mapToObj(index -> stock("00000" + index, BigDecimal.valueOf(7 - index).toPlainString(), "60000000000", 1_000_000 + index))
                .toList();
        ClosingBetCandidateScanner scanner = scanner(
                new FakeRankingPort(stocks, stocks, stocks),
                signalStore,
                new RecordingNotificationPort()
        );

        ClosingBetCandidateScanResult result = scanner.scan(TRADE_DATE, 5);

        assertThat(result.selectedCount()).isEqualTo(5);
        assertThat(result.selectedCandidates()).hasSize(5);
    }

    @Test
    void returnsFewerThanFiveWhenOnlyFewerCandidatesQualify() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        List<MarketRankingStock> stocks = List.of(
                stock("005930", "5.1", "60000000000", 1_000_000),
                stock("000660", "4.2", "55000000000", 900_000)
        );
        ClosingBetCandidateScanner scanner = scanner(
                new FakeRankingPort(stocks, stocks, stocks),
                signalStore,
                new RecordingNotificationPort()
        );

        ClosingBetCandidateScanResult result = scanner.scan(TRADE_DATE, 5);

        assertThat(result.selectedCount()).isEqualTo(2);
        assertThat(result.selectedCandidates()).extracting("stockCode")
                .containsExactly("005930", "000660");
    }

    @Test
    void savesSelectedCandidatesAsTradingSignals() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        MarketRankingStock stock = stock("005930", "5.0", "60000000000", 1_000_000);
        ClosingBetCandidateScanner scanner = scanner(
                new FakeRankingPort(List.of(stock), List.of(stock), List.of(stock)),
                signalStore,
                new RecordingNotificationPort()
        );

        scanner.scan(TRADE_DATE, 5);

        assertThat(signalStore.saved)
                .singleElement()
                .satisfies(signal -> {
                    assertThat(signal.strategyName()).isEqualTo(ClosingBetCandidateScanner.STRATEGY_NAME);
                    assertThat(signal.signalType()).isEqualTo(SignalType.BUY_CANDIDATE);
                    assertThat(signal.reasons())
                            .contains("MARKET_SCAN_14_00", "TRADING_VALUE_TOP", "VOLUME_SURGE");
                });
    }

    private static ClosingBetCandidateScanner scanner(
            MarketRankingPort rankingPort,
            InMemorySignalStore signalStore,
            NotificationPort notificationPort
    ) {
        return new ClosingBetCandidateScanner(
                rankingPort,
                signalStore,
                signalStore,
                notificationPort,
                Clock.fixed(Instant.parse("2026-06-05T05:00:00Z"), ZoneOffset.UTC)
        );
    }

    private static MarketRankingStock stock(String stockCode, String changeRate, String tradingValue, long volume) {
        return new MarketRankingStock(
                stockCode,
                "stock-" + stockCode,
                Market.KOSPI,
                BigDecimal.valueOf(50_000),
                new BigDecimal(changeRate),
                new BigDecimal(tradingValue),
                volume
        );
    }

    private record FakeRankingPort(
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

    private static class InMemorySignalStore implements TradingSignalPort, TradingSignalQueryPort {
        private final List<TradingSignal> saved = new ArrayList<>();

        @Override
        public TradingSignal save(TradingSignal tradingSignal) {
            saved.add(tradingSignal);
            return tradingSignal;
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
            return records;
        }
    }

    private static class RecordingNotificationPort implements NotificationPort {
        @Override
        public NotificationDeliveryResult send(NotificationMessage message) {
            return NotificationDeliveryResult.success();
        }
    }
}
