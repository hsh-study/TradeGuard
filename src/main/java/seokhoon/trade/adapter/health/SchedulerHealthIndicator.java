package seokhoon.trade.adapter.health;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.service.ClosingBetCandidateScanScheduler;
import seokhoon.trade.application.service.ClosingBetFinalReviewScheduler;

@Component("scheduler")
public class SchedulerHealthIndicator implements HealthIndicator {
    private final ObjectProvider<ClosingBetCandidateScanScheduler> scanScheduler;
    private final ObjectProvider<ClosingBetFinalReviewScheduler> reviewScheduler;
    private final ObjectProvider<MarketCalendarPort> marketCalendar;

    public SchedulerHealthIndicator(
            ObjectProvider<ClosingBetCandidateScanScheduler> scanScheduler,
            ObjectProvider<ClosingBetFinalReviewScheduler> reviewScheduler,
            ObjectProvider<MarketCalendarPort> marketCalendar
    ) {
        this.scanScheduler = scanScheduler;
        this.reviewScheduler = reviewScheduler;
        this.marketCalendar = marketCalendar;
    }

    @Override
    public Health health() {
        boolean scanLoaded = scanScheduler.getIfAvailable() != null;
        boolean reviewLoaded = reviewScheduler.getIfAvailable() != null;
        boolean calendarLoaded = marketCalendar.getIfAvailable() != null;
        Health.Builder builder = scanLoaded && reviewLoaded && calendarLoaded
                ? Health.up()
                : Health.down();
        return builder
                .withDetail("candidateScanSchedulerLoaded", scanLoaded)
                .withDetail("finalReviewSchedulerLoaded", reviewLoaded)
                .withDetail("marketCalendarLoaded", calendarLoaded)
                .build();
    }
}
