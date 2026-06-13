package seokhoon.trade.application.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import seokhoon.trade.adapter.metrics.MicrometerOperationalMetricsAdapter;
import seokhoon.trade.application.port.in.EarlyMarketScanResult;
import seokhoon.trade.application.port.out.CorrelationIdProvider;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryRecord;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EarlyMarketPreOpenSchedulerTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-10T00:30:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void recordsSuccessHistoryAndMetrics() {
        RecordingHistory history = new RecordingHistory();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EarlyMarketPreOpenScheduler scheduler = new EarlyMarketPreOpenScheduler(
                (tradeDate, limit) -> new EarlyMarketScanResult(
                        tradeDate, 12, 10, false, "summary", List.of()
                ),
                date -> true,
                history,
                new MicrometerOperationalMetricsAdapter(registry),
                fixedCorrelation(),
                CLOCK
        );

        scheduler.scanPreOpen();

        assertThat(history.startedName).isEqualTo(SchedulerName.EARLY_MARKET_PRE_OPEN_830);
        assertThat(history.scannedCount).isEqualTo(12);
        assertThat(history.selectedCount).isEqualTo(10);
        assertThat(history.correlationId).isEqualTo("early-830-correlation");
        assertThat(registry.find("tradeguard.scheduler.execution.count")
                .tag("schedulerName", "EARLY_MARKET_PRE_OPEN_830")
                .tag("status", "SUCCEEDED")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void skipsOnNonTradingDay() {
        RecordingHistory history = new RecordingHistory();
        EarlyMarketPreOpenScheduler scheduler = new EarlyMarketPreOpenScheduler(
                (tradeDate, limit) -> {
                    throw new AssertionError("use case must not run");
                },
                date -> false,
                history,
                seokhoon.trade.application.port.out.OperationalMetricsPort.noop(),
                fixedCorrelation(),
                CLOCK
        );

        scheduler.scanPreOpen();

        assertThat(history.skippedName).isEqualTo(SchedulerName.EARLY_MARKET_PRE_OPEN_830);
        assertThat(history.skipReason).isEqualTo("NON_TRADING_DAY");
    }

    @Test
    void recordsFailureAndRethrows() {
        RecordingHistory history = new RecordingHistory();
        EarlyMarketPreOpenScheduler scheduler = new EarlyMarketPreOpenScheduler(
                (tradeDate, limit) -> {
                    throw new IllegalStateException("ranking unavailable");
                },
                date -> true,
                history,
                seokhoon.trade.application.port.out.OperationalMetricsPort.noop(),
                fixedCorrelation(),
                CLOCK
        );

        assertThatThrownBy(scheduler::scanPreOpen)
                .isInstanceOf(IllegalStateException.class);
        assertThat(history.failedId).isEqualTo(1L);
        assertThat(history.failureReason).contains("ranking unavailable");
    }

    @Test
    void continuesStrategyWhenRawDataCaptureFails() {
        RecordingHistory history = new RecordingHistory();
        AtomicBoolean strategyExecuted = new AtomicBoolean();
        EarlyMarketPreOpenScheduler scheduler = new EarlyMarketPreOpenScheduler(
                (tradeDate, limit) -> {
                    strategyExecuted.set(true);
                    return new EarlyMarketScanResult(
                            tradeDate, 1, 1, false, "summary", List.of()
                    );
                },
                tradeDate -> {
                    throw new IllegalStateException("archive unavailable");
                },
                date -> true,
                history,
                seokhoon.trade.application.port.out.OperationalMetricsPort.noop(),
                fixedCorrelation(),
                CLOCK
        );

        scheduler.scanPreOpen();

        assertThat(strategyExecuted).isTrue();
        assertThat(history.scannedCount).isEqualTo(1);
        assertThat(history.failedId).isZero();
    }

    private static CorrelationIdProvider fixedCorrelation() {
        return new CorrelationIdProvider() {
            @Override
            public String currentCorrelationId() {
                return "early-830-correlation";
            }

            @Override
            public String newCorrelationId() {
                return "early-830-correlation";
            }
        };
    }

    private static class RecordingHistory implements SchedulerExecutionHistoryPort {
        private SchedulerName startedName;
        private SchedulerName skippedName;
        private String skipReason;
        private String correlationId;
        private int scannedCount;
        private int selectedCount;
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
            this.correlationId = correlationId;
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
            this.correlationId = correlationId;
        }

        @Override
        public void markFailed(long historyId, String failureReason, Instant finishedAt) {
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
