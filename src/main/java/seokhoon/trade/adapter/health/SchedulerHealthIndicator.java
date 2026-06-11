package seokhoon.trade.adapter.health;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.service.ClosingBetCandidateScanScheduler;
import seokhoon.trade.application.service.ClosingBetFinalReviewScheduler;
import seokhoon.trade.application.service.EarlyMarketOpeningScheduler;
import seokhoon.trade.application.service.EarlyMarketFollowUpScheduler;
import seokhoon.trade.application.service.EarlyMarketPerformanceCaptureScheduler;
import seokhoon.trade.application.service.EarlyMarketPreOpenScheduler;

@Component("scheduler")
public class SchedulerHealthIndicator implements HealthIndicator {
    private final ObjectProvider<ClosingBetCandidateScanScheduler> scanScheduler;
    private final ObjectProvider<ClosingBetFinalReviewScheduler> reviewScheduler;
    private final ObjectProvider<EarlyMarketPreOpenScheduler> earlyPreOpenScheduler;
    private final ObjectProvider<EarlyMarketOpeningScheduler> earlyOpeningScheduler;
    private final ObjectProvider<EarlyMarketFollowUpScheduler> earlyFollowUpScheduler;
    private final ObjectProvider<EarlyMarketPerformanceCaptureScheduler>
            earlyPerformanceCaptureScheduler;
    private final ObjectProvider<MarketCalendarPort> marketCalendar;

    public SchedulerHealthIndicator(
            ObjectProvider<ClosingBetCandidateScanScheduler> scanScheduler,
            ObjectProvider<ClosingBetFinalReviewScheduler> reviewScheduler,
            ObjectProvider<EarlyMarketPreOpenScheduler> earlyPreOpenScheduler,
            ObjectProvider<EarlyMarketOpeningScheduler> earlyOpeningScheduler,
            ObjectProvider<EarlyMarketFollowUpScheduler> earlyFollowUpScheduler,
            ObjectProvider<EarlyMarketPerformanceCaptureScheduler>
                    earlyPerformanceCaptureScheduler,
            ObjectProvider<MarketCalendarPort> marketCalendar
    ) {
        this.scanScheduler = scanScheduler;
        this.reviewScheduler = reviewScheduler;
        this.earlyPreOpenScheduler = earlyPreOpenScheduler;
        this.earlyOpeningScheduler = earlyOpeningScheduler;
        this.earlyFollowUpScheduler = earlyFollowUpScheduler;
        this.earlyPerformanceCaptureScheduler = earlyPerformanceCaptureScheduler;
        this.marketCalendar = marketCalendar;
    }

    @Override
    public Health health() {
        boolean scanLoaded = scanScheduler.getIfAvailable() != null;
        boolean reviewLoaded = reviewScheduler.getIfAvailable() != null;
        boolean earlyPreOpenLoaded = earlyPreOpenScheduler.getIfAvailable() != null;
        boolean earlyOpeningLoaded = earlyOpeningScheduler.getIfAvailable() != null;
        boolean earlyFollowUpLoaded = earlyFollowUpScheduler.getIfAvailable() != null;
        boolean earlyPerformanceCaptureLoaded =
                earlyPerformanceCaptureScheduler.getIfAvailable() != null;
        boolean calendarLoaded = marketCalendar.getIfAvailable() != null;
        Health.Builder builder = scanLoaded
                && reviewLoaded
                && earlyPreOpenLoaded
                && earlyOpeningLoaded
                && earlyFollowUpLoaded
                && earlyPerformanceCaptureLoaded
                && calendarLoaded
                ? Health.up()
                : Health.down();
        return builder
                .withDetail("candidateScanSchedulerLoaded", scanLoaded)
                .withDetail("finalReviewSchedulerLoaded", reviewLoaded)
                .withDetail("earlyMarketPreOpenSchedulerLoaded", earlyPreOpenLoaded)
                .withDetail("earlyMarketOpeningSchedulerLoaded", earlyOpeningLoaded)
                .withDetail("earlyMarketFollowUpSchedulerLoaded", earlyFollowUpLoaded)
                .withDetail(
                        "earlyMarketPerformanceCaptureSchedulerLoaded",
                        earlyPerformanceCaptureLoaded
                )
                .withDetail("marketCalendarLoaded", calendarLoaded)
                .build();
    }
}
