package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.LiveTradingUseCases;
import seokhoon.trade.config.LiveTradingProperties;

import static org.mockito.Mockito.*;

class LiveOrderReconciliationSchedulerTest {
    @Test
    void skipsWhenKisTradingIsDisabled() {
        var useCase=mock(LiveTradingUseCases.ReconcileLiveOrdersUseCase.class);
        var properties=new LiveTradingProperties();
        new LiveOrderReconciliationScheduler(useCase,properties).reconcile();
        verifyNoInteractions(useCase);
    }

    @Test
    void reconcilesExistingOrdersWhenKisIsEnabledEvenIfNewOrdersAreDisabled() {
        var useCase=mock(LiveTradingUseCases.ReconcileLiveOrdersUseCase.class);
        var properties=new LiveTradingProperties();
        properties.setKisTradingEnabled(true);
        when(useCase.reconcile()).thenReturn(2);
        new LiveOrderReconciliationScheduler(useCase,properties).reconcile();
        verify(useCase).reconcile();
    }
}
