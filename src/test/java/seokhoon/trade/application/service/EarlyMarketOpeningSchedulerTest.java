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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EarlyMarketOpeningSchedulerTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-10T00:05:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void recordsSuccessHistoryAndMetrics() {
        RecordingHistory history = new RecordingHistory();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EarlyMarketOpeningScheduler scheduler = new EarlyMarketOpeningScheduler(
                (tradeDate, limit) -> new EarlyMarketScanResult(
                        tradeDate, 10, 3, true, "summary", List.of()
                ),
                date -> true,
                history,
                new MicrometerOperationalMetricsAdapter(registry),
                fixedCorrelation(),
                CLOCK
        );

        scheduler.compressAfterOpen();

        assertThat(history.startedName).isEqualTo(SchedulerName.EARLY_MARKET_OPENING_905);
        assertThat(history.scannedCount).isEqualTo(10);
        assertThat(history.selectedCount).isEqualTo(3);
        assertThat(registry.find("tradeguard.scheduler.execution.count")
                .tag("schedulerName", "EARLY_MARKET_OPENING_905")
                .tag("status", "SUCCEEDED")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void skipsOnNonTradingDay() {
        RecordingHistory history = new RecordingHistory();
        EarlyMarketOpeningScheduler scheduler = new EarlyMarketOpeningScheduler(
                (tradeDate, limit) -> {
                    throw new AssertionError("use case must not run");
                },
                date -> false,
                history,
                seokhoon.trade.application.port.out.OperationalMetricsPort.noop(),
                fixedCorrelation(),
                CLOCK
        );

        scheduler.compressAfterOpen();

        assertThat(history.skippedName).isEqualTo(SchedulerName.EARLY_MARKET_OPENING_905);
        assertThat(history.skipReason).isEqualTo("NON_TRADING_DAY");
    }

    @Test
    void recordsFailureAndRethrows() {
        RecordingHistory history = new RecordingHistory();
        EarlyMarketOpeningScheduler scheduler = new EarlyMarketOpeningScheduler(
                (tradeDate, limit) -> {
                    throw new IllegalStateException("snapshot unavailable");
                },
                date -> true,
                history,
                seokhoon.trade.application.port.out.OperationalMetricsPort.noop(),
                fixedCorrelation(),
                CLOCK
        );

        assertThatThrownBy(scheduler::compressAfterOpen)
                .isInstanceOf(IllegalStateException.class);
        assertThat(history.failedId).isEqualTo(1L);
        assertThat(history.failureReason).contains("snapshot unavailable");
    }

    private static CorrelationIdProvider fixedCorrelation() {
        return new CorrelationIdProvider() {
            @Override
            public String currentCorrelationId() {
                return "early-905-correlation";
            }

            @Override
            public String newCorrelationId() {
                return "early-905-correlation";
            }
        };
    }

    private static class RecordingHistory implements SchedulerExecutionHistoryPort {
        private SchedulerName startedName;
        private SchedulerName skippedName;
        private String skipReason;
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
