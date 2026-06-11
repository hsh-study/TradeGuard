package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.MarketCalendarSyncResult;
import seokhoon.trade.application.port.in.SyncMarketCalendarUseCase;
import seokhoon.trade.application.port.out.CorrelationIdProvider;
import seokhoon.trade.application.port.out.MarketCalendarDayPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.domain.market.MarketCalendarSource;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketCalendarSyncSchedulerTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-11T19:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void skipsWhenCurrentAndNextYearsExist() {
        SyncMarketCalendarUseCase useCase = mock(SyncMarketCalendarUseCase.class);
        MarketCalendarDayPort dayPort = mock(MarketCalendarDayPort.class);
        SchedulerExecutionHistoryPort history =
                mock(SchedulerExecutionHistoryPort.class);
        when(dayPort.existsByYear(2026)).thenReturn(true);
        when(dayPort.existsByYear(2027)).thenReturn(true);
        MarketCalendarSyncScheduler scheduler = scheduler(useCase, dayPort, history);

        scheduler.syncMissingYears();

        verify(useCase, never()).syncYear(anyInt());
        verify(history).markSkipped(
                eq(SchedulerName.MARKET_CALENDAR_SYNC),
                eq(LocalDate.of(2026, 6, 12)),
                eq("CALENDAR_YEARS_ALREADY_EXIST"),
                eq("calendar-sync"),
                any()
        );
    }

    @Test
    void syncsOnlyMissingYearAndRecordsCounts() {
        SyncMarketCalendarUseCase useCase = mock(SyncMarketCalendarUseCase.class);
        MarketCalendarDayPort dayPort = mock(MarketCalendarDayPort.class);
        SchedulerExecutionHistoryPort history =
                mock(SchedulerExecutionHistoryPort.class);
        when(dayPort.existsByYear(2026)).thenReturn(true);
        when(dayPort.existsByYear(2027)).thenReturn(false);
        when(history.saveStarted(any(), any(), any(), any())).thenReturn(7L);
        when(useCase.syncYear(2027)).thenReturn(new MarketCalendarSyncResult(
                365,
                250,
                115,
                MarketCalendarSource.FALLBACK_GENERATED,
                List.of("fallback")
        ));
        MarketCalendarSyncScheduler scheduler = scheduler(useCase, dayPort, history);

        scheduler.syncMissingYears();

        verify(useCase).syncYear(2027);
        verify(history).markSucceeded(7L, 365, 365, false, Instant.now(CLOCK));
    }

    private static MarketCalendarSyncScheduler scheduler(
            SyncMarketCalendarUseCase useCase,
            MarketCalendarDayPort dayPort,
            SchedulerExecutionHistoryPort history
    ) {
        return new MarketCalendarSyncScheduler(
                useCase,
                dayPort,
                history,
                seokhoon.trade.application.port.out.OperationalMetricsPort.noop(),
                new CorrelationIdProvider() {
                    @Override
                    public String currentCorrelationId() {
                        return "calendar-sync";
                    }

                    @Override
                    public String newCorrelationId() {
                        return "calendar-sync";
                    }
                },
                CLOCK
        );
    }
}
