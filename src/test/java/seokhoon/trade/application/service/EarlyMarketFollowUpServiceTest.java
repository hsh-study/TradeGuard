package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpDecision;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.IntradayMarketSnapshot;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.market.IntradayBar;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyMarketFollowUpServiceTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-10T00:20:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void classifiesKeepCautionAndExcludeFromIntradayBars() {
        RecordingNotification notification = new RecordingNotification(true);
        EarlyMarketFollowUpService service = service(
                List.of(
                        signal(1L, "KEEP", 95),
                        signal(2L, "CAUTION", 90),
                        signal(3L, "EXCLUDE", 85)
                ),
                Map.of(
                        "KEEP", List.of(
                                bar("KEEP", "09:05", "100", "101", "99", "100.5", "99")
                        ),
                        "CAUTION", List.of(
                                bar("CAUTION", "09:05", "100", "101", "98", "99", "100"),
                                bar("CAUTION", "09:20", "99", "101", "99", "100.5", "100")
                        ),
                        "EXCLUDE", List.of(
                                bar("EXCLUDE", "09:05", "100", "101", "99", "100", "99"),
                                bar("EXCLUDE", "09:20", "100", "100", "97", "98", "99")
                        )
                ),
                Map.of(),
                notification
        );

        var result = service.followUp(TRADE_DATE);

        assertThat(result.checkedCount()).isEqualTo(3);
        assertThat(result.keepCount()).isEqualTo(1);
        assertThat(result.cautionCount()).isEqualTo(1);
        assertThat(result.excludeCount()).isEqualTo(1);
        assertThat(result.briefingSent()).isTrue();
        assertThat(result.candidates())
                .extracting(candidate -> candidate.stockCode() + ":" + candidate.decision())
                .containsExactly(
                        "KEEP:" + EarlyMarketFollowUpDecision.KEEP,
                        "CAUTION:" + EarlyMarketFollowUpDecision.CAUTION,
                        "EXCLUDE:" + EarlyMarketFollowUpDecision.EXCLUDE
                );
        assertThat(result.candidates().get(1).reasons())
                .contains("VWAP_BROKEN_DURING_WINDOW");
        assertThat(result.candidates().get(2).reasons())
                .contains("LAST_PRICE_BELOW_VWAP");
        assertThat(notification.message.body())
                .contains("KEEP: 1")
                .contains("CAUTION: 1")
                .contains("EXCLUDE: 1")
                .contains("LAST_PRICE_BELOW_VWAP")
                .contains("주문은 생성하지 않습니다");
    }

    @Test
    void fallsBackToSnapshotWhenBarsAreMissing() {
        EarlyMarketFollowUpService service = service(
                List.of(signal(1L, "005930", 90)),
                Map.of(),
                Map.of("005930", snapshot("005930", "99", "100", "98")),
                message -> NotificationDeliveryResult.skipped(
                        "discord webhook url is not configured"
                )
        );

        var result = service.followUp(TRADE_DATE);

        assertThat(result.briefingSent()).isFalse();
        assertThat(result.cautionCount()).isEqualTo(1);
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.decision()).isEqualTo(EarlyMarketFollowUpDecision.CAUTION);
            assertThat(candidate.reasons()).contains(
                    "SNAPSHOT_PROXY",
                    "DRAWDOWN_FROM_HIGH_1_TO_2_PERCENT"
            );
            assertThat(candidate.drawdownFromHigh()).isEqualByComparingTo("-1.0000");
        });
    }

    @Test
    void marksInsufficientSnapshotDataAsCaution() {
        EarlyMarketFollowUpService service = service(
                List.of(signal(1L, "005930", 90)),
                Map.of(),
                Map.of(),
                message -> NotificationDeliveryResult.skipped("disabled")
        );

        var candidate = service.followUp(TRADE_DATE).candidates().getFirst();

        assertThat(candidate.decision()).isEqualTo(EarlyMarketFollowUpDecision.CAUTION);
        assertThat(candidate.reasons()).contains("DATA_INSUFFICIENT");
        assertThat(candidate.lastPrice()).isNull();
    }

    private static EarlyMarketFollowUpService service(
            List<TradingSignalRecord> signals,
            Map<String, List<IntradayBar>> bars,
            Map<String, IntradayMarketSnapshot> snapshots,
            NotificationPort notification
    ) {
        return new EarlyMarketFollowUpService(
                (TradingSignalSearchCriteria criteria) -> signals,
                (stockCode, tradeDate, from, to, interval) ->
                        bars.getOrDefault(stockCode, List.of()),
                stockCode -> Optional.ofNullable(snapshots.get(stockCode)),
                notification,
                OperationalMetricsPort.noop(),
                CLOCK
        );
    }

    private static TradingSignalRecord signal(long id, String stockCode, int score) {
        return new TradingSignalRecord(
                id,
                EarlyMarketPreOpenScanner.STRATEGY_NAME,
                stockCode,
                TRADE_DATE,
                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                score,
                List.of(),
                List.of(),
                TradingSignalStatus.CREATED
        );
    }

    private static IntradayBar bar(
            String stockCode,
            String time,
            String open,
            String high,
            String low,
            String close,
            String vwap
    ) {
        return new IntradayBar(
                stockCode,
                TRADE_DATE,
                LocalTime.parse(time),
                new BigDecimal(open),
                new BigDecimal(high),
                new BigDecimal(low),
                new BigDecimal(close),
                100,
                BigDecimal.valueOf(10_000),
                new BigDecimal(vwap)
        );
    }

    private static IntradayMarketSnapshot snapshot(
            String stockCode,
            String currentPrice,
            String high,
            String vwap
    ) {
        return new IntradayMarketSnapshot(
                stockCode,
                new BigDecimal(currentPrice),
                BigDecimal.ZERO,
                new BigDecimal(high),
                new BigDecimal("95"),
                100,
                BigDecimal.valueOf(10_000),
                new BigDecimal(vwap),
                CLOCK.instant()
        );
    }

    private static class RecordingNotification implements NotificationPort {
        private final boolean sent;
        private NotificationMessage message;

        private RecordingNotification(boolean sent) {
            this.sent = sent;
        }

        @Override
        public NotificationDeliveryResult send(NotificationMessage message) {
            this.message = message;
            return sent
                    ? NotificationDeliveryResult.success()
                    : NotificationDeliveryResult.skipped("disabled");
        }
    }
}
