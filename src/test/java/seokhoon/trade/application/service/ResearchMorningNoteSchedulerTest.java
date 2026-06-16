package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.ResearchProperties;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchMorningNoteSchedulerTest {
    @Test
    void skipsOnNonTradingDay() {
        RecordingHistory history = new RecordingHistory();
        ResearchMorningNoteScheduler scheduler = new ResearchMorningNoteScheduler(
                new seokhoon.trade.application.port.in.ResearchUseCases.MorningNoteUseCase() {
                    @Override
                    public seokhoon.trade.domain.research.MorningNote generate(LocalDate tradeDate) {
                        throw new AssertionError("generation must not run");
                    }

                    @Override
                    public seokhoon.trade.domain.research.MorningNote load(LocalDate tradeDate) {
                        throw new UnsupportedOperationException();
                    }
                },
                date -> false,
                history,
                OperationalMetricsPort.noop(),
                new CorrelationIdProvider() {
                    public String currentCorrelationId() { return "research"; }
                    public String newCorrelationId() { return "research"; }
                },
                message -> NotificationDeliveryResult.skipped("disabled"),
                new ResearchProperties(),
                Clock.fixed(Instant.parse("2026-06-14T23:10:00Z"), ZoneId.of("Asia/Seoul"))
        );

        scheduler.generateMorningNote();

        assertThat(history.schedulerName).isEqualTo(SchedulerName.RESEARCH_MORNING_NOTE);
        assertThat(history.reason).isEqualTo("NON_TRADING_DAY");
    }

    private static class RecordingHistory implements SchedulerExecutionHistoryPort {
        private SchedulerName schedulerName;
        private String reason;

        public long saveStarted(SchedulerName name, LocalDate date, String correlationId, Instant at) {
            return 1;
        }
        public void markSucceeded(long id, int scanned, int selected, boolean sent, Instant at) {}
        public void markSkipped(SchedulerName name, LocalDate date, String reason, String correlationId, Instant at) {
            this.schedulerName = name;
            this.reason = reason;
        }
        public void markFailed(long id, String reason, Instant at) {}
        public List<SchedulerExecutionHistoryRecord> find(
                LocalDate date, SchedulerName name, SchedulerExecutionStatus status
        ) {
            return List.of();
        }
    }
}
