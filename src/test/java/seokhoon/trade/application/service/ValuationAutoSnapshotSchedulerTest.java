package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.GenerateValuationSnapshotUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.ResearchProperties;
import seokhoon.trade.domain.research.ValuationGenerationResult;
import seokhoon.trade.domain.research.ValuationGenerationStatus;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValuationAutoSnapshotSchedulerTest {
    @Test
    void skipsOnNonTradingDay() {
        RecordingHistory history = new RecordingHistory();
        CountingUseCase useCase = new CountingUseCase();
        ValuationAutoSnapshotScheduler scheduler = new ValuationAutoSnapshotScheduler(
                useCase,
                date -> false,
                history,
                OperationalMetricsPort.noop(),
                new StaticCorrelationIdProvider(),
                new ResearchProperties(),
                Clock.fixed(Instant.parse("2026-06-14T22:55:00Z"), ZoneId.of("Asia/Seoul"))
        );

        scheduler.generateWatchlistValuationSnapshots();

        assertThat(history.schedulerName).isEqualTo(SchedulerName.VALUATION_AUTO_SNAPSHOT);
        assertThat(history.reason).isEqualTo("NON_TRADING_DAY");
        assertThat(useCase.count).isZero();
    }

    private static class CountingUseCase implements GenerateValuationSnapshotUseCase {
        private int count;
        @Override public ValuationGenerationResult generate(String stockCode, LocalDate baseDate) {
            return null;
        }
        @Override public List<ValuationGenerationResult> generateBatch(List<String> stockCodes, LocalDate baseDate) {
            return List.of();
        }
        @Override public List<ValuationGenerationResult> generateWatchlist(LocalDate baseDate) {
            count++;
            return List.of(new ValuationGenerationResult("005930", baseDate,
                    ValuationGenerationStatus.GENERATED, null, List.of()));
        }
    }

    private static class StaticCorrelationIdProvider implements CorrelationIdProvider {
        @Override public String currentCorrelationId() { return "valuation"; }
        @Override public String newCorrelationId() { return "valuation"; }
    }

    private static class RecordingHistory implements SchedulerExecutionHistoryPort {
        private SchedulerName schedulerName;
        private String reason;

        @Override public long saveStarted(SchedulerName name, LocalDate date, String correlationId, Instant at) {
            return 1;
        }
        @Override public void markSucceeded(long id, int scanned, int selected, boolean sent, Instant at) {}
        @Override public void markSkipped(SchedulerName name, LocalDate date, String reason, String correlationId, Instant at) {
            this.schedulerName = name;
            this.reason = reason;
        }
        @Override public void markFailed(long id, String reason, Instant at) {}
        @Override public List<SchedulerExecutionHistoryRecord> find(
                LocalDate date, SchedulerName name, SchedulerExecutionStatus status
        ) {
            return List.of();
        }
    }
}
