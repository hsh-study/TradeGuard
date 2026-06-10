package seokhoon.trade.application.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import seokhoon.trade.adapter.metrics.MicrometerOperationalMetricsAdapter;
import seokhoon.trade.application.port.in.EarlyMarketPerformanceCaptureResult;
import seokhoon.trade.application.port.in.EarlyMarketPerformanceView;
import seokhoon.trade.application.port.out.CorrelationIdProvider;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryRecord;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EarlyMarketPerformanceCaptureSchedulerTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-10T00:31:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void recordsSucceededHistoryAndSendsPerformanceBriefing() {
        RecordingHistory history = new RecordingHistory();
        RecordingNotification notification = new RecordingNotification(false);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EarlyMarketPerformanceCaptureScheduler scheduler = scheduler(
                tradeDate -> result(),
                date -> true,
                notification,
                history,
                new MicrometerOperationalMetricsAdapter(registry)
        );

        scheduler.captureAfterOpeningWindow();

        assertThat(history.startedName)
                .isEqualTo(SchedulerName.EARLY_MARKET_PERFORMANCE_CAPTURE_930);
        assertThat(history.scannedCount).isEqualTo(3);
        assertThat(history.selectedCount).isEqualTo(3);
        assertThat(history.notificationSent).isFalse();
        assertThat(notification.message.title()).contains("09:30 장초반 성과 요약");
        assertThat(notification.message.body())
                .contains("bars_used: 2")
                .contains("snapshot_proxy: 1")
                .contains("vwapBroken 후보 수: 1")
                .contains("stockCode=000660")
                .contains("maxReturnRateUntil0930=8.5%");
        assertThat(registry.find("tradeguard.scheduler.execution.count")
                .tag("schedulerName", "EARLY_MARKET_PERFORMANCE_CAPTURE_930")
                .tag("status", "SUCCEEDED")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.find("tradeguard.scheduler.notification.sent.count")
                .tag("schedulerName", "EARLY_MARKET_PERFORMANCE_CAPTURE_930")
                .tag("sent", "false")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordsSkippedHistoryOnNonTradingDay() {
        RecordingHistory history = new RecordingHistory();
        EarlyMarketPerformanceCaptureScheduler scheduler = scheduler(
                tradeDate -> {
                    throw new AssertionError("capture use case must not run");
                },
                date -> false,
                message -> {
                    throw new AssertionError("notification must not run");
                },
                history,
                OperationalMetricsPort.noop()
        );

        scheduler.captureAfterOpeningWindow();

        assertThat(history.skippedName)
                .isEqualTo(SchedulerName.EARLY_MARKET_PERFORMANCE_CAPTURE_930);
        assertThat(history.skipReason).isEqualTo("NON_TRADING_DAY");
    }

    @Test
    void recordsFailedHistoryWhenCaptureUseCaseThrows() {
        RecordingHistory history = new RecordingHistory();
        EarlyMarketPerformanceCaptureScheduler scheduler = scheduler(
                tradeDate -> {
                    throw new IllegalStateException("intraday capture failed");
                },
                date -> true,
                message -> NotificationDeliveryResult.success(),
                history,
                OperationalMetricsPort.noop()
        );

        assertThatThrownBy(scheduler::captureAfterOpeningWindow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("intraday capture failed");
        assertThat(history.failedId).isEqualTo(1L);
        assertThat(history.failureReason).contains("intraday capture failed");
    }

    @Test
    void treatsDiscordNoOpAsSuccessfulSchedulerExecution() {
        RecordingHistory history = new RecordingHistory();
        EarlyMarketPerformanceCaptureScheduler scheduler = scheduler(
                tradeDate -> result(),
                date -> true,
                message -> NotificationDeliveryResult.skipped(
                        "discord webhook url is not configured"
                ),
                history,
                OperationalMetricsPort.noop()
        );

        scheduler.captureAfterOpeningWindow();

        assertThat(history.scannedCount).isEqualTo(3);
        assertThat(history.selectedCount).isEqualTo(3);
        assertThat(history.notificationSent).isFalse();
        assertThat(history.failedId).isZero();
    }

    private static EarlyMarketPerformanceCaptureScheduler scheduler(
            seokhoon.trade.application.port.in.CaptureEarlyMarketPerformancesUseCase useCase,
            seokhoon.trade.application.port.out.MarketCalendarPort calendar,
            NotificationPort notification,
            RecordingHistory history,
            OperationalMetricsPort metrics
    ) {
        return new EarlyMarketPerformanceCaptureScheduler(
                useCase,
                calendar,
                notification,
                history,
                metrics,
                fixedCorrelation(),
                CLOCK
        );
    }

    private static EarlyMarketPerformanceCaptureResult result() {
        return new EarlyMarketPerformanceCaptureResult(
                TRADE_DATE,
                3,
                3,
                List.of(
                        performance(1L, "005930", "5.25", false, true),
                        performance(2L, "000660", "8.50", true, true),
                        performance(3L, "035420", null, false, false)
                )
        );
    }

    private static EarlyMarketPerformanceView performance(
            long signalId,
            String stockCode,
            String maxReturnRate,
            boolean vwapBroken,
            boolean barsUsed
    ) {
        return new EarlyMarketPerformanceView(
                signalId,
                stockCode,
                TRADE_DATE,
                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                90,
                barsUsed ? BigDecimal.valueOf(100) : null,
                barsUsed ? BigDecimal.valueOf(110) : null,
                barsUsed ? BigDecimal.valueOf(95) : null,
                BigDecimal.valueOf(105),
                maxReturnRate == null ? null : new BigDecimal(maxReturnRate),
                barsUsed ? new BigDecimal("-5.0") : null,
                vwapBroken,
                CLOCK.instant()
        );
    }

    private static CorrelationIdProvider fixedCorrelation() {
        return new CorrelationIdProvider() {
            @Override
            public String currentCorrelationId() {
                return "early-performance-correlation";
            }

            @Override
            public String newCorrelationId() {
                return "early-performance-correlation";
            }
        };
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
                    : NotificationDeliveryResult.skipped(
                            "discord webhook url is not configured"
                    );
        }
    }

    private static class RecordingHistory implements SchedulerExecutionHistoryPort {
        private SchedulerName startedName;
        private SchedulerName skippedName;
        private String skipReason;
        private int scannedCount;
        private int selectedCount;
        private boolean notificationSent;
        private long failedId;
        private String failureReason;

        @Override
        public long saveStarted(
                SchedulerName schedulerName,
                LocalDate tradeDate,
                String correlationId,
                Instant startedAt
        ) {
            this.startedName = schedulerName;
            return 1L;
        }

        @Override
        public void markSucceeded(
                long historyId,
                int scannedCount,
                int selectedCount,
                boolean notificationSent,
                Instant finishedAt
        ) {
            this.scannedCount = scannedCount;
            this.selectedCount = selectedCount;
            this.notificationSent = notificationSent;
        }

        @Override
        public void markSkipped(
                SchedulerName schedulerName,
                LocalDate tradeDate,
                String skipReason,
                String correlationId,
                Instant occurredAt
        ) {
            this.skippedName = schedulerName;
            this.skipReason = skipReason;
        }

        @Override
        public void markFailed(
                long historyId,
                String failureReason,
                Instant finishedAt
        ) {
            this.failedId = historyId;
            this.failureReason = failureReason;
        }

        @Override
        public List<SchedulerExecutionHistoryRecord> find(
                LocalDate tradeDate,
                SchedulerName schedulerName,
                SchedulerExecutionStatus status
        ) {
            return List.of();
        }
    }
}
