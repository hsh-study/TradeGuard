package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ClosingBetFinalReviewResult;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.IntradayMarketSnapshot;
import seokhoon.trade.application.port.out.MarketSnapshotPort;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.strategy.ClosingBetStrategy;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClosingBetFinalReviewServiceTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 5);

    @Test
    void savesCandidateWhenSnapshotIsAboveVwapAndNearIntradayHigh() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        signalStore.addPreScan(preScan(1L, "005930", 75, List.of()));
        FakeSnapshotPort snapshotPort = new FakeSnapshotPort();
        snapshotPort.add(goodSnapshot("005930", "76000"));
        ClosingBetFinalReviewService service = service(
                signalStore,
                snapshotPort,
                new RecordingNotificationPort()
        );

        ClosingBetFinalReviewResult result = service.review(TRADE_DATE, 5);

        assertThat(result.reviewedCount()).isEqualTo(1);
        assertThat(result.selectedCount()).isEqualTo(1);
        assertThat(signalStore.saved)
                .singleElement()
                .satisfies(signal -> {
                    assertThat(signal.strategyName()).isEqualTo(ClosingBetStrategy.STRATEGY_NAME);
                    assertThat(signal.stockCode()).isEqualTo("005930");
                    assertThat(signal.score()).isEqualTo(90);
                    assertThat(signal.reasons())
                            .contains(
                                    "FINAL_REVIEW_15_00",
                                    "PRE_SCAN_CONFIRMED",
                                    "ABOVE_VWAP",
                                    "NEAR_INTRADAY_HIGH",
                                    "ACCUMULATED_TRADING_VALUE_OVER_50B_KRW"
                            );
                });
    }

    @Test
    void excludesPreScanCandidatesWithRiskReasons() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        signalStore.addPreScan(preScan(1L, "005930", 90, List.of("DUPLICATE_ORDER")));
        FakeSnapshotPort snapshotPort = new FakeSnapshotPort();
        snapshotPort.add(goodSnapshot("005930", "76000"));
        ClosingBetFinalReviewService service = service(
                signalStore,
                snapshotPort,
                new RecordingNotificationPort()
        );

        ClosingBetFinalReviewResult result = service.review(TRADE_DATE, 5);

        assertThat(result.selectedCount()).isZero();
        assertThat(signalStore.saved).isEmpty();
    }

    @Test
    void excludesCandidateWhenSnapshotIsBelowVwap() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        signalStore.addPreScan(preScan(1L, "005930", 90, List.of()));
        FakeSnapshotPort snapshotPort = new FakeSnapshotPort();
        snapshotPort.add(snapshot("005930", "74000", "76000", "76000", "60000000000"));
        ClosingBetFinalReviewService service = service(
                signalStore,
                snapshotPort,
                new RecordingNotificationPort()
        );

        ClosingBetFinalReviewResult result = service.review(TRADE_DATE, 5);

        assertThat(result.selectedCount()).isZero();
        assertThat(signalStore.saved).isEmpty();
    }

    @Test
    void excludesCandidateWhenPricePulledBackMoreThanFivePercentFromHigh() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        signalStore.addPreScan(preScan(1L, "005930", 90, List.of()));
        FakeSnapshotPort snapshotPort = new FakeSnapshotPort();
        snapshotPort.add(snapshot("005930", "90000", "88000", "100000", "60000000000"));
        ClosingBetFinalReviewService service = service(
                signalStore,
                snapshotPort,
                new RecordingNotificationPort()
        );

        ClosingBetFinalReviewResult result = service.review(TRADE_DATE, 5);

        assertThat(result.selectedCount()).isZero();
        assertThat(signalStore.saved).isEmpty();
    }

    @Test
    void excludesCandidateWhenSnapshotIsUnavailable() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        signalStore.addPreScan(preScan(1L, "005930", 90, List.of()));
        ClosingBetFinalReviewService service = service(
                signalStore,
                stockCode -> Optional.empty(),
                new RecordingNotificationPort()
        );

        ClosingBetFinalReviewResult result = service.review(TRADE_DATE, 5);

        assertThat(result.selectedCount()).isZero();
        assertThat(signalStore.saved).isEmpty();
    }

    @Test
    void savesOnlyTopFiveFinalCandidates() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        FakeSnapshotPort snapshotPort = new FakeSnapshotPort();
        for (int index = 0; index < 6; index++) {
            signalStore.addPreScan(preScan((long) index + 1, "00000" + index, 90 - index, List.of()));
            snapshotPort.add(goodSnapshot("00000" + index, Integer.toString(76000 - index * 100)));
        }
        ClosingBetFinalReviewService service = service(
                signalStore,
                snapshotPort,
                new RecordingNotificationPort()
        );

        ClosingBetFinalReviewResult result = service.review(TRADE_DATE, 5);

        assertThat(result.selectedCount()).isEqualTo(5);
        assertThat(signalStore.saved).hasSize(5);
        assertThat(result.selectedCandidates())
                .extracting("stockCode")
                .containsExactly("000000", "000001", "000002", "000003", "000004");
    }

    @Test
    void sendsNoCandidateBriefingWhenNoCandidateSelected() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        signalStore.addPreScan(preScan(1L, "005930", 74, List.of()));
        RecordingNotificationPort notificationPort = new RecordingNotificationPort();
        ClosingBetFinalReviewService service = service(
                signalStore,
                stockCode -> Optional.empty(),
                notificationPort
        );

        ClosingBetFinalReviewResult result = service.review(TRADE_DATE, 5);

        assertThat(result.selectedCount()).isZero();
        assertThat(result.summary()).isEqualTo("15:00 최종 후보 없음");
        assertThat(result.selectedCandidates()).isEmpty();
        assertThat(notificationPort.message.body()).contains("최종 후보 없음");
    }

    private static ClosingBetFinalReviewService service(
            InMemorySignalStore signalStore,
            MarketSnapshotPort marketSnapshotPort,
            NotificationPort notificationPort
    ) {
        return new ClosingBetFinalReviewService(
                signalStore,
                signalStore,
                marketSnapshotPort,
                notificationPort,
                Clock.fixed(Instant.parse("2026-06-05T06:00:00Z"), ZoneOffset.UTC)
        );
    }

    private static IntradayMarketSnapshot goodSnapshot(String stockCode, String currentPrice) {
        return snapshot(stockCode, currentPrice, "74000", "77000", "60000000000");
    }

    private static IntradayMarketSnapshot snapshot(
            String stockCode,
            String currentPrice,
            String vwap,
            String intradayHigh,
            String accumulatedTradingValue
    ) {
        return new IntradayMarketSnapshot(
                stockCode,
                new BigDecimal(currentPrice),
                BigDecimal.valueOf(5),
                new BigDecimal(intradayHigh),
                BigDecimal.valueOf(70000),
                1_000_000,
                new BigDecimal(accumulatedTradingValue),
                new BigDecimal(vwap),
                Instant.parse("2026-06-05T06:00:00Z")
        );
    }

    private static TradingSignalRecord preScan(
            Long id,
            String stockCode,
            int score,
            List<String> riskReasons
    ) {
        return new TradingSignalRecord(
                id,
                ClosingBetCandidateScanner.STRATEGY_NAME,
                stockCode,
                TRADE_DATE,
                SignalType.BUY_CANDIDATE,
                score,
                List.of("MARKET_SCAN_14_00"),
                riskReasons,
                TradingSignalStatus.CREATED
        );
    }

    private static class InMemorySignalStore implements TradingSignalPort, TradingSignalQueryPort {
        private final List<TradingSignalRecord> preScanSignals = new ArrayList<>();
        private final List<TradingSignal> saved = new ArrayList<>();

        private void addPreScan(TradingSignalRecord record) {
            preScanSignals.add(record);
        }

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
            if (ClosingBetCandidateScanner.STRATEGY_NAME.equals(criteria.strategyName())) {
                return preScanSignals.stream()
                        .filter(record -> record.score() >= criteria.minScore())
                        .toList();
            }
            if (ClosingBetStrategy.STRATEGY_NAME.equals(criteria.strategyName())) {
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

    private static class RecordingNotificationPort implements NotificationPort {
        private NotificationMessage message;

        @Override
        public NotificationDeliveryResult send(NotificationMessage message) {
            this.message = message;
            return NotificationDeliveryResult.success();
        }
    }

    private static class FakeSnapshotPort implements MarketSnapshotPort {
        private final Map<String, IntradayMarketSnapshot> snapshots = new HashMap<>();

        private void add(IntradayMarketSnapshot snapshot) {
            snapshots.put(snapshot.stockCode(), snapshot);
        }

        @Override
        public Optional<IntradayMarketSnapshot> getSnapshot(String stockCode) {
            return Optional.ofNullable(snapshots.get(stockCode));
        }
    }
}
