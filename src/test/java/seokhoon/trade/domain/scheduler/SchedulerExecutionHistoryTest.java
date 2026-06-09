package seokhoon.trade.domain.scheduler;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchedulerExecutionHistoryTest {
    @Test
    void transitionsStartedExecutionToSucceeded() {
        SchedulerExecutionHistory history = SchedulerExecutionHistory.started(
                SchedulerName.CLOSING_BET_PRE_SCAN_14,
                LocalDate.of(2026, 6, 5),
                "scheduler-correlation-success",
                Instant.parse("2026-06-05T05:00:00Z")
        );

        history.markSucceeded(
                12,
                2,
                true,
                Instant.parse("2026-06-05T05:00:03Z")
        );

        assertThat(history.status()).isEqualTo(SchedulerExecutionStatus.SUCCEEDED);
        assertThat(history.scannedCount()).isEqualTo(12);
        assertThat(history.selectedCount()).isEqualTo(2);
        assertThat(history.notificationSent()).isTrue();
        assertThat(history.correlationId()).isEqualTo("scheduler-correlation-success");
    }

    @Test
    void doesNotFinishExecutionTwice() {
        SchedulerExecutionHistory history = SchedulerExecutionHistory.started(
                SchedulerName.CLOSING_BET_FINAL_REVIEW_15,
                LocalDate.of(2026, 6, 5),
                "scheduler-correlation-failed",
                Instant.parse("2026-06-05T06:00:00Z")
        );
        history.markFailed(
                "snapshot unavailable",
                Instant.parse("2026-06-05T06:00:01Z")
        );

        assertThatThrownBy(() -> history.markSucceeded(
                7,
                1,
                false,
                Instant.parse("2026-06-05T06:00:02Z")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("only STARTED scheduler executions can finish");
    }
}
