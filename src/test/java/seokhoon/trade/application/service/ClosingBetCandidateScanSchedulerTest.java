package seokhoon.trade.application.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import seokhoon.trade.adapter.metrics.MicrometerOperationalMetricsAdapter;
import seokhoon.trade.application.port.out.CorrelationIdProvider;
import seokhoon.trade.application.port.in.ClosingBetCandidateScanResult;
import seokhoon.trade.application.port.in.ScanClosingBetCandidatesUseCase;
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

class ClosingBetCandidateScanSchedulerTest {
    @Test
    void delegatesScheduledScanToUseCase() {
        RecordingScanUseCase useCase = new RecordingScanUseCase();
        RecordingHistoryPort historyPort = new RecordingHistoryPort();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ClosingBetCandidateScanScheduler scheduler = new ClosingBetCandidateScanScheduler(
                useCase,
                date -> true,
                historyPort,
                new MicrometerOperationalMetricsAdapter(registry),
                fixedCorrelationId("scheduler-correlation-14"),
                Clock.fixed(Instant.parse("2026-06-05T05:00:00Z"), ZoneId.of("Asia/Seoul"))
        );

        scheduler.scanAtMarketAfternoon();

        assertThat(useCase.tradeDate).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(useCase.limit).isEqualTo(5);
        assertThat(useCase.invocationCount).isEqualTo(1);
        assertThat(historyPort.startedName).isEqualTo(SchedulerName.CLOSING_BET_PRE_SCAN_14);
        assertThat(historyPort.succeededHistoryId).isEqualTo(10L);
        assertThat(historyPort.scannedCount).isEqualTo(12);
        assertThat(historyPort.selectedCount).isEqualTo(2);
        assertThat(historyPort.notificationSent).isTrue();
        assertThat(historyPort.correlationId).isEqualTo("scheduler-correlation-14");
        assertThat(useCase.correlationId).isEqualTo("scheduler-correlation-14");
        assertThat(registry.find("tradeguard.scheduler.execution.count")
                .tag("schedulerName", "CLOSING_BET_PRE_SCAN_14")
                .tag("status", "SUCCEEDED")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(registry.find("tradeguard.scheduler.selected.count")
                .tag("schedulerName", "CLOSING_BET_PRE_SCAN_14")
                .counter()
                .count()).isEqualTo(2.0);
    }

    @Test
    void skipsScheduledScanOnNonTradingDay() {
        RecordingScanUseCase useCase = new RecordingScanUseCase();
        RecordingHistoryPort historyPort = new RecordingHistoryPort();
        ClosingBetCandidateScanScheduler scheduler = new ClosingBetCandidateScanScheduler(
                useCase,
                date -> false,
                historyPort,
                Clock.fixed(Instant.parse("2026-06-06T05:00:00Z"), ZoneId.of("Asia/Seoul"))
        );

        scheduler.scanAtMarketAfternoon();

        assertThat(useCase.invocationCount).isZero();
        assertThat(historyPort.skippedName).isEqualTo(SchedulerName.CLOSING_BET_PRE_SCAN_14);
        assertThat(historyPort.skipReason).isEqualTo("NON_TRADING_DAY");
        assertThat(historyPort.correlationId).isNotBlank();
        assertThat(historyPort.startedName).isNull();
    }

    @Test
    void recordsFailedAndRethrowsWhenScheduledScanFails() {
        RecordingHistoryPort historyPort = new RecordingHistoryPort();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ClosingBetCandidateScanScheduler scheduler = new ClosingBetCandidateScanScheduler(
                (tradeDate, limit) -> {
                    throw new IllegalStateException("ranking unavailable");
                },
                date -> true,
                historyPort,
                new MicrometerOperationalMetricsAdapter(registry),
                Clock.fixed(Instant.parse("2026-06-05T05:00:00Z"), ZoneId.of("Asia/Seoul"))
        );

        assertThatThrownBy(scheduler::scanAtMarketAfternoon)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ranking unavailable");
        assertThat(historyPort.failedHistoryId).isEqualTo(10L);
        assertThat(historyPort.failureReason)
                .isEqualTo("IllegalStateException: ranking unavailable");
        assertThat(registry.find("tradeguard.scheduler.execution.count")
                .tag("schedulerName", "CLOSING_BET_PRE_SCAN_14")
                .tag("status", "FAILED")
                .counter()
                .count()).isEqualTo(1.0);
    }

    private static class RecordingScanUseCase implements ScanClosingBetCandidatesUseCase {
        private LocalDate tradeDate;
        private int limit;
        private int invocationCount;
        private String correlationId;

        @Override
        public ClosingBetCandidateScanResult scan(LocalDate tradeDate, int limit) {
            invocationCount++;
            this.tradeDate = tradeDate;
            this.limit = limit;
            this.correlationId = MDC.get("correlationId");
            return new ClosingBetCandidateScanResult(
                    tradeDate,
                    12,
                    7,
                    2,
                    true,
                    "summary",
                    List.of()
            );
        }
    }

    private static CorrelationIdProvider fixedCorrelationId(String correlationId) {
        return new CorrelationIdProvider() {
            @Override
            public String currentCorrelationId() {
                return correlationId;
            }

            @Override
            public String newCorrelationId() {
                return correlationId;
            }
        };
    }

    private static class RecordingHistoryPort implements SchedulerExecutionHistoryPort {
        private SchedulerName startedName;
        private SchedulerName skippedName;
        private String skipReason;
        private long succeededHistoryId;
        private int scannedCount;
        private int selectedCount;
        private boolean notificationSent;
        private long failedHistoryId;
        private String failureReason;
        private String correlationId;

        @Override
        public long saveStarted(
                SchedulerName schedulerName,
                LocalDate tradeDate,
                String correlationId,
                Instant startedAt
        ) {
            startedName = schedulerName;
            this.correlationId = correlationId;
            return 10L;
        }

        @Override
        public void markSucceeded(
                long historyId,
                int scannedCount,
                int selectedCount,
                boolean notificationSent,
                Instant finishedAt
        ) {
            succeededHistoryId = historyId;
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
            skippedName = schedulerName;
            this.skipReason = skipReason;
            this.correlationId = correlationId;
        }

        @Override
        public void markFailed(long historyId, String failureReason, Instant finishedAt) {
            failedHistoryId = historyId;
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
