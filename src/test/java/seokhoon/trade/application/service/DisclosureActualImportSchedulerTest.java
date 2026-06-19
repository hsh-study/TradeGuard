package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ImportDisclosureActualEvidenceUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.DisclosureActualProviderProperties;
import seokhoon.trade.domain.scheduler.*;

import java.time.*;

import static org.mockito.Mockito.*;

class DisclosureActualImportSchedulerTest {
    @Test void skipsNonTradingDayWithoutCallingProviderUseCase() {
        ImportDisclosureActualEvidenceUseCase useCase=mock(ImportDisclosureActualEvidenceUseCase.class);
        MarketCalendarPort calendar=mock(MarketCalendarPort.class);
        SchedulerExecutionHistoryPort histories=mock(SchedulerExecutionHistoryPort.class);
        CorrelationIdProvider correlations=mock(CorrelationIdProvider.class);
        when(calendar.isTradingDay(LocalDate.of(2026,6,14))).thenReturn(false);
        DisclosureActualImportScheduler scheduler=new DisclosureActualImportScheduler(useCase,calendar,histories,
                OperationalMetricsPort.noop(),correlations,new DisclosureActualProviderProperties(),
                Clock.fixed(Instant.parse("2026-06-13T15:00:00Z"),ZoneId.of("Asia/Seoul")));
        scheduler.execute("cid");
        verify(histories).markSkipped(SchedulerName.DISCLOSURE_ACTUAL_IMPORT,LocalDate.of(2026,6,14),
                "NON_TRADING_DAY","cid",Instant.parse("2026-06-13T15:00:00Z"));
        verifyNoInteractions(useCase);
    }
}
