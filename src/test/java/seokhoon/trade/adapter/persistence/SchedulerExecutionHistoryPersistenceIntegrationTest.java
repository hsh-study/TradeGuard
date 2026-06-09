package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchedulerExecutionHistoryPersistenceIntegrationTest {
    @Autowired
    private SchedulerExecutionHistoryPort historyPort;

    @Autowired
    private SchedulerExecutionHistoryJpaRepository repository;

    @BeforeEach
    void clearHistories() {
        repository.deleteAll();
    }

    @Test
    void savesStartedAndMarksSucceeded() {
        LocalDate tradeDate = LocalDate.of(2026, 6, 5);
        long id = historyPort.saveStarted(
                SchedulerName.CLOSING_BET_PRE_SCAN_14,
                tradeDate,
                Instant.parse("2026-06-05T05:00:00Z")
        );

        historyPort.markSucceeded(
                id,
                12,
                2,
                true,
                Instant.parse("2026-06-05T05:00:03Z")
        );

        assertThat(historyPort.find(
                tradeDate,
                SchedulerName.CLOSING_BET_PRE_SCAN_14,
                SchedulerExecutionStatus.SUCCEEDED
        ))
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.id()).isEqualTo(id);
                    assertThat(history.scannedCount()).isEqualTo(12);
                    assertThat(history.selectedCount()).isEqualTo(2);
                    assertThat(history.notificationSent()).isTrue();
                    assertThat(history.finishedAt())
                            .isEqualTo(Instant.parse("2026-06-05T05:00:03Z"));
                });
    }

    @Test
    void savesSkippedAndFailedExecutionsAndFiltersLatestFirst() {
        LocalDate tradeDate = LocalDate.of(2026, 6, 6);
        historyPort.markSkipped(
                SchedulerName.CLOSING_BET_PRE_SCAN_14,
                tradeDate,
                "NON_TRADING_DAY",
                Instant.parse("2026-06-06T05:00:00Z")
        );
        long failedId = historyPort.saveStarted(
                SchedulerName.CLOSING_BET_FINAL_REVIEW_15,
                tradeDate,
                Instant.parse("2026-06-06T06:00:00Z")
        );
        historyPort.markFailed(
                failedId,
                "IllegalStateException: snapshot unavailable",
                Instant.parse("2026-06-06T06:00:01Z")
        );

        assertThat(historyPort.find(tradeDate, null, null))
                .extracting(history -> history.status())
                .containsExactly(
                        SchedulerExecutionStatus.FAILED,
                        SchedulerExecutionStatus.SKIPPED
                );
        assertThat(historyPort.find(
                tradeDate,
                SchedulerName.CLOSING_BET_PRE_SCAN_14,
                SchedulerExecutionStatus.SKIPPED
        ))
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.skipReason()).isEqualTo("NON_TRADING_DAY");
                    assertThat(history.finishedAt()).isNotNull();
                });
    }
}
