package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ResearchUseCases;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.market.Sector;
import seokhoon.trade.domain.market.SectorDailySnapshot;
import seokhoon.trade.domain.market.StockSectorMapping;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SectorDailySnapshotSchedulerTest {
    @Test
    void skipsOnNonTradingDay() {
        RecordingHistory history = new RecordingHistory();
        RecordingSectorUseCase useCase = new RecordingSectorUseCase();
        SectorDailySnapshotScheduler scheduler = new SectorDailySnapshotScheduler(
                useCase,
                date -> false,
                history,
                OperationalMetricsPort.noop(),
                correlationIds(),
                Clock.fixed(Instant.parse("2026-06-14T23:05:00Z"), ZoneId.of("Asia/Seoul"))
        );

        scheduler.generateSectorDailySnapshot();

        assertThat(useCase.called).isFalse();
        assertThat(history.schedulerName).isEqualTo(SchedulerName.SECTOR_DAILY_SNAPSHOT);
        assertThat(history.reason).isEqualTo("NON_TRADING_DAY");
    }

    @Test
    void generatesPreviousTradingDaySnapshotBeforeMorningNote() {
        RecordingHistory history = new RecordingHistory();
        RecordingSectorUseCase useCase = new RecordingSectorUseCase();
        SectorDailySnapshotScheduler scheduler = new SectorDailySnapshotScheduler(
                useCase,
                date -> !date.getDayOfWeek().equals(DayOfWeek.SATURDAY)
                        && !date.getDayOfWeek().equals(DayOfWeek.SUNDAY),
                history,
                OperationalMetricsPort.noop(),
                correlationIds(),
                Clock.fixed(Instant.parse("2026-06-14T23:05:00Z"), ZoneId.of("Asia/Seoul"))
        );

        scheduler.generateSectorDailySnapshot();

        assertThat(useCase.called).isTrue();
        assertThat(useCase.tradeDate).isEqualTo(LocalDate.of(2026, 6, 12));
        assertThat(history.date).isEqualTo(LocalDate.of(2026, 6, 12));
    }

    private static CorrelationIdProvider correlationIds() {
        return new CorrelationIdProvider() {
            public String currentCorrelationId() { return "sector"; }
            public String newCorrelationId() { return "sector"; }
        };
    }

    private static class RecordingSectorUseCase implements ResearchUseCases.SectorUseCase {
        private boolean called;
        private LocalDate tradeDate;

        @Override
        public Sector create(ResearchUseCases.CreateSectorCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StockSectorMapping addStock(String sectorCode, ResearchUseCases.AddSectorStockCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Sector> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SectorDailySnapshot loadSnapshot(String sectorCode, LocalDate tradeDate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResearchUseCases.SectorSnapshotGenerationResult generateSnapshots(LocalDate tradeDate) {
            this.called = true;
            this.tradeDate = tradeDate;
            return new ResearchUseCases.SectorSnapshotGenerationResult(tradeDate, 2, 2, 0);
        }
    }

    private static class RecordingHistory implements SchedulerExecutionHistoryPort {
        private SchedulerName schedulerName;
        private LocalDate date;
        private String reason;

        public long saveStarted(SchedulerName name, LocalDate date, String correlationId, Instant at) {
            this.schedulerName = name;
            this.date = date;
            return 1;
        }
        public void markSucceeded(long id, int scanned, int selected, boolean sent, Instant at) {}
        public void markSkipped(SchedulerName name, LocalDate date, String reason, String correlationId, Instant at) {
            this.schedulerName = name;
            this.date = date;
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
