package seokhoon.trade.adapter.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Status;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.service.ClosingBetCandidateScanScheduler;
import seokhoon.trade.application.service.ClosingBetFinalReviewScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchedulerHealthIndicatorTest {
    @Test
    void reportsUpWhenBothSchedulersAndCalendarAreLoaded() {
        SchedulerHealthIndicator indicator = new SchedulerHealthIndicator(
                providerWith(mock(ClosingBetCandidateScanScheduler.class)),
                providerWith(mock(ClosingBetFinalReviewScheduler.class)),
                providerWith(mock(MarketCalendarPort.class))
        );

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("candidateScanSchedulerLoaded", true)
                .containsEntry("finalReviewSchedulerLoaded", true)
                .containsEntry("marketCalendarLoaded", true);
    }

    @Test
    void reportsDownWhenRequiredSchedulerDependencyIsMissing() {
        SchedulerHealthIndicator indicator = new SchedulerHealthIndicator(
                providerWith(mock(ClosingBetCandidateScanScheduler.class)),
                providerWith(null),
                providerWith(mock(MarketCalendarPort.class))
        );

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> providerWith(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
