package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ImportMarketIndexUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.MarketIndexProviderProperties;
import seokhoon.trade.domain.market.MarketIndexImportHistory;
import seokhoon.trade.domain.market.MarketIndexImportProvider;
import seokhoon.trade.domain.market.MarketIndexImportStatus;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MarketIndexImportSchedulerTest {
    @Test
    void skipsWhenAutoRunDisabled() {
        ImportMarketIndexUseCase useCase = mock(ImportMarketIndexUseCase.class);
        MarketCalendarPort calendar = mock(MarketCalendarPort.class);
        RecordingHistory history = new RecordingHistory();
        OperationalMetricsPort metrics = mock(OperationalMetricsPort.class);
        MarketIndexProviderProperties properties = new MarketIndexProviderProperties();
        MarketIndexImportScheduler scheduler = new MarketIndexImportScheduler(useCase, calendar,
                history, metrics, correlationIds(), properties,
                Clock.fixed(Instant.parse("2026-06-14T23:00:00Z"), ZoneId.of("Asia/Seoul")));

        scheduler.importMarketIndices();

        assertThat(history.schedulerName).isEqualTo(SchedulerName.MARKET_INDEX_IMPORT);
        assertThat(history.status).isEqualTo(SchedulerExecutionStatus.SKIPPED);
        assertThat(history.reason).isEqualTo("DISABLED");
        verifyNoInteractions(useCase);
    }

    @Test
    void importsPreviousTradingDayWhenEnabled() {
        ImportMarketIndexUseCase useCase = mock(ImportMarketIndexUseCase.class);
        MarketCalendarPort calendar = mock(MarketCalendarPort.class);
        RecordingHistory history = new RecordingHistory();
        OperationalMetricsPort metrics = mock(OperationalMetricsPort.class);
        MarketIndexProviderProperties properties = new MarketIndexProviderProperties();
        properties.setImportAutoRun(true);
        properties.setEnabled(true);
        LocalDate today = LocalDate.of(2026, 6, 15);
        LocalDate previous = LocalDate.of(2026, 6, 12);
        when(calendar.isTradingDay(today)).thenReturn(true);
        when(calendar.previousTradingDay(today)).thenReturn(previous);
        when(useCase.importProvider(previous)).thenReturn(new MarketIndexImportHistory(1L,
                MarketIndexImportProvider.KIS, previous, MarketIndexImportStatus.SUCCESS,
                2, null, Instant.now(), Instant.now()));
        MarketIndexImportScheduler scheduler = new MarketIndexImportScheduler(useCase, calendar,
                history, metrics, correlationIds(), properties,
                Clock.fixed(Instant.parse("2026-06-14T23:00:00Z"), ZoneId.of("Asia/Seoul")));

        scheduler.importMarketIndices();

        assertThat(history.schedulerName).isEqualTo(SchedulerName.MARKET_INDEX_IMPORT);
        assertThat(history.status).isEqualTo(SchedulerExecutionStatus.SUCCEEDED);
        verify(useCase).importProvider(previous);
    }

    private static class RecordingHistory implements SchedulerExecutionHistoryPort {
        private SchedulerName schedulerName;
        private SchedulerExecutionStatus status;
        private String reason;

        @Override
        public long saveStarted(SchedulerName name, LocalDate date, String correlationId, Instant at) {
            schedulerName = name;
            status = SchedulerExecutionStatus.STARTED;
            return 1L;
        }

        @Override
        public void markSucceeded(long id, int candidateCount, int selectedCount, boolean notificationSent, Instant at) {
            status = SchedulerExecutionStatus.SUCCEEDED;
        }

        @Override
        public void markFailed(long id, String failureReason, Instant at) {
            status = SchedulerExecutionStatus.FAILED;
            reason = failureReason;
        }

        @Override
        public void markSkipped(SchedulerName name, LocalDate date, String reason, String correlationId, Instant at) {
            schedulerName = name;
            status = SchedulerExecutionStatus.SKIPPED;
            this.reason = reason;
        }

        @Override
        public List<SchedulerExecutionHistoryRecord> find(
                LocalDate date, SchedulerName name, SchedulerExecutionStatus status
        ) {
            return List.of();
        }
    }

    private static CorrelationIdProvider correlationIds() {
        return new CorrelationIdProvider() {
            @Override
            public String currentCorrelationId() {
                return "corr";
            }

            @Override
            public String newCorrelationId() {
                return "corr";
            }
        };
    }
}
