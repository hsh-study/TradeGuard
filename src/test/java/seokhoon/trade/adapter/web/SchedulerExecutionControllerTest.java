package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.SchedulerExecutionHistoryView;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerExecutionControllerTest {
    @Test
    void returnsFilteredSchedulerExecutionHistories() {
        LocalDate tradeDate = LocalDate.of(2026, 6, 5);
        SchedulerExecutionController controller = new SchedulerExecutionController(
                (date, schedulerName, status) -> {
                    assertThat(date).isEqualTo(tradeDate);
                    assertThat(schedulerName)
                            .isEqualTo(SchedulerName.CLOSING_BET_PRE_SCAN_14);
                    assertThat(status).isEqualTo(SchedulerExecutionStatus.SUCCEEDED);
                    return List.of(new SchedulerExecutionHistoryView(
                            10L,
                            schedulerName,
                            date,
                            status,
                            null,
                            null,
                            12,
                            2,
                            true,
                            Instant.parse("2026-06-05T05:00:00Z"),
                            Instant.parse("2026-06-05T05:00:03Z")
                    ));
                }
        );

        var response = controller.find(
                tradeDate,
                SchedulerName.CLOSING_BET_PRE_SCAN_14,
                SchedulerExecutionStatus.SUCCEEDED
        );

        assertThat(response).singleElement().satisfies(history -> {
            assertThat(history.id()).isEqualTo(10L);
            assertThat(history.scannedCount()).isEqualTo(12);
            assertThat(history.selectedCount()).isEqualTo(2);
            assertThat(history.notificationSent()).isTrue();
        });
    }
}
