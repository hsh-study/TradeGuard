package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.EarlyMarketScanResult;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.IntradayMarketSnapshot;
import seokhoon.trade.application.port.out.MarketSnapshotPort;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyMarketOpeningCompressorTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);

    @Test
    void savesEntryCandidateWhenAboveVwapNearHighAndTradingValueIsSufficient() {
        SignalStore store = new SignalStore();
        store.preScans.add(preScan(1L, "005930"));
        SnapshotPort snapshots = new SnapshotPort();
        snapshots.add(snapshot("005930", "100", "95", "102", "40000000000"));
        EarlyMarketOpeningCompressor compressor = compressor(store, snapshots);

        EarlyMarketScanResult result = compressor.compress(TRADE_DATE, 3);

        assertThat(result.selectedCount()).isEqualTo(1);
        assertThat(store.saved).singleElement().satisfies(signal -> {
            assertThat(signal.signalType()).isEqualTo(SignalType.EARLY_MARKET_ENTRY_CANDIDATE);
            assertThat(signal.strategyName()).isEqualTo("EARLY_MARKET_BREAKOUT");
            assertThat(signal.score()).isEqualTo(105);
            assertThat(signal.reasons()).contains(
                    "ABOVE_VWAP",
                    "NEAR_INTRADAY_HIGH",
                    "ACCUMULATED_TRADING_VALUE_SUFFICIENT"
            );
        });
    }

    @Test
    void excludesCandidateBelowVwapOrFarFromHigh() {
        SignalStore store = new SignalStore();
        store.preScans.add(preScan(1L, "005930"));
        SnapshotPort snapshots = new SnapshotPort();
        snapshots.add(snapshot("005930", "80", "90", "100", "40000000000"));

        EarlyMarketScanResult result = compressor(store, snapshots).compress(TRADE_DATE, 3);

        assertThat(result.selectedCount()).isZero();
        assertThat(store.saved).isEmpty();
    }

    @Test
    void excludesPreScanCandidateWithRiskReasons() {
        SignalStore store = new SignalStore();
        store.preScans.add(new TradingSignalRecord(
                1L,
                EarlyMarketPreOpenScanner.STRATEGY_NAME,
                "005930",
                TRADE_DATE,
                SignalType.EARLY_MARKET_PRE_SCAN,
                80,
                List.of("EARLY_MARKET_PRE_OPEN_08_30"),
                List.of("ONLY_BUY_CANDIDATE_SUPPORTED_IN_MVP"),
                TradingSignalStatus.RISK_REJECTED
        ));
        SnapshotPort snapshots = new SnapshotPort();
        snapshots.add(snapshot("005930", "100", "95", "102", "40000000000"));

        EarlyMarketScanResult result = compressor(store, snapshots).compress(TRADE_DATE, 3);

        assertThat(result.selectedCount()).isZero();
        assertThat(store.saved).isEmpty();
    }

    @Test
    void limitsCompressedCandidatesToThreeAndSupportsNoOpBriefing() {
        SignalStore store = new SignalStore();
        SnapshotPort snapshots = new SnapshotPort();
        for (int index = 0; index < 5; index++) {
            String stockCode = "00000" + index;
            store.preScans.add(preScan((long) index + 1, stockCode));
            snapshots.add(snapshot(stockCode, "100", "95", "102", "40000000000"));
        }
        EarlyMarketOpeningCompressor compressor = new EarlyMarketOpeningCompressor(
                store,
                store,
                snapshots,
                message -> NotificationDeliveryResult.skipped("disabled"),
                Clock.fixed(Instant.parse("2026-06-10T00:05:00Z"), ZoneOffset.UTC)
        );

        EarlyMarketScanResult result = compressor.compress(TRADE_DATE, 3);

        assertThat(result.selectedCount()).isEqualTo(3);
        assertThat(result.briefingSent()).isFalse();
        assertThat(store.saved).hasSize(3);
    }

    private static EarlyMarketOpeningCompressor compressor(
            SignalStore store,
            MarketSnapshotPort snapshots
    ) {
        return new EarlyMarketOpeningCompressor(
                store,
                store,
                snapshots,
                message -> NotificationDeliveryResult.success(),
                Clock.fixed(Instant.parse("2026-06-10T00:05:00Z"), ZoneOffset.UTC)
        );
    }

    private static TradingSignalRecord preScan(Long id, String stockCode) {
        return new TradingSignalRecord(
                id,
                EarlyMarketPreOpenScanner.STRATEGY_NAME,
                stockCode,
                TRADE_DATE,
                SignalType.EARLY_MARKET_PRE_SCAN,
                80,
                List.of("EARLY_MARKET_PRE_OPEN_08_30"),
                List.of(),
                TradingSignalStatus.CREATED
        );
    }

    private static IntradayMarketSnapshot snapshot(
            String stockCode,
            String currentPrice,
            String vwap,
            String high,
            String tradingValue
    ) {
        return new IntradayMarketSnapshot(
                stockCode,
                new BigDecimal(currentPrice),
                BigDecimal.valueOf(3),
                new BigDecimal(high),
                BigDecimal.valueOf(70),
                1_000_000,
                new BigDecimal(tradingValue),
                new BigDecimal(vwap),
                Instant.parse("2026-06-10T00:05:00Z")
        );
    }

    private static class SnapshotPort implements MarketSnapshotPort {
        private final Map<String, IntradayMarketSnapshot> values = new HashMap<>();

        void add(IntradayMarketSnapshot snapshot) {
            values.put(snapshot.stockCode(), snapshot);
        }

        @Override
        public Optional<IntradayMarketSnapshot> getSnapshot(String stockCode) {
            return Optional.ofNullable(values.get(stockCode));
        }
    }

    private static class SignalStore implements TradingSignalPort, TradingSignalQueryPort {
        private final List<TradingSignalRecord> preScans = new ArrayList<>();
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
            if (criteria.signalType() == SignalType.EARLY_MARKET_PRE_SCAN) {
                return preScans;
            }
            if (criteria.signalType() == SignalType.EARLY_MARKET_ENTRY_CANDIDATE) {
                List<TradingSignalRecord> records = new ArrayList<>();
                for (int index = 0; index < saved.size(); index++) {
                    TradingSignal signal = saved.get(index);
                    records.add(new TradingSignalRecord(
                            100L + index,
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
            return List.of();
        }
    }
}
