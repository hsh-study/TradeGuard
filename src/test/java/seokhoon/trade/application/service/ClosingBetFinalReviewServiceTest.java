package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ClosingBetFinalReviewResult;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClosingBetFinalReviewServiceTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 5);

    @Test
    void savesOnlyPreScanCandidatesWithScoreAtLeastSeventyFiveAsFinalCandidates() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        signalStore.addPreScan(preScan(1L, "005930", 75, List.of()));
        signalStore.addPreScan(preScan(2L, "000660", 74, List.of()));
        ClosingBetFinalReviewService service = service(signalStore, new RecordingNotificationPort());

        ClosingBetFinalReviewResult result = service.review(TRADE_DATE, 5);

        assertThat(result.reviewedCount()).isEqualTo(2);
        assertThat(result.selectedCount()).isEqualTo(1);
        assertThat(signalStore.saved)
                .singleElement()
                .satisfies(signal -> {
                    assertThat(signal.strategyName()).isEqualTo(ClosingBetStrategy.STRATEGY_NAME);
                    assertThat(signal.stockCode()).isEqualTo("005930");
                    assertThat(signal.score()).isEqualTo(75);
                    assertThat(signal.reasons())
                            .contains("FINAL_REVIEW_15_00", "PRE_SCAN_CONFIRMED");
                });
    }

    @Test
    void excludesPreScanCandidatesWithRiskReasons() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        signalStore.addPreScan(preScan(1L, "005930", 90, List.of("DUPLICATE_ORDER")));
        ClosingBetFinalReviewService service = service(signalStore, new RecordingNotificationPort());

        ClosingBetFinalReviewResult result = service.review(TRADE_DATE, 5);

        assertThat(result.selectedCount()).isZero();
        assertThat(signalStore.saved).isEmpty();
    }

    @Test
    void savesOnlyTopFiveFinalCandidates() {
        InMemorySignalStore signalStore = new InMemorySignalStore();
        for (int index = 0; index < 6; index++) {
            signalStore.addPreScan(preScan((long) index + 1, "00000" + index, 90 - index, List.of()));
        }
        ClosingBetFinalReviewService service = service(signalStore, new RecordingNotificationPort());

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
        ClosingBetFinalReviewService service = service(signalStore, notificationPort);

        ClosingBetFinalReviewResult result = service.review(TRADE_DATE, 5);

        assertThat(result.selectedCount()).isZero();
        assertThat(result.summary()).isEqualTo("15:00 최종 후보 없음");
        assertThat(result.selectedCandidates()).isEmpty();
        assertThat(notificationPort.message.body()).contains("최종 후보 없음");
    }

    private static ClosingBetFinalReviewService service(
            InMemorySignalStore signalStore,
            NotificationPort notificationPort
    ) {
        return new ClosingBetFinalReviewService(
                signalStore,
                signalStore,
                notificationPort,
                Clock.fixed(Instant.parse("2026-06-05T06:00:00Z"), ZoneOffset.UTC)
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
}
