package seokhoon.trade.adapter.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerOperationalMetricsAdapterTest {
    @Test
    void recordsOperationalMetricsWithBoundedNonSensitiveTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerOperationalMetricsAdapter metrics =
                new MicrometerOperationalMetricsAdapter(registry);

        metrics.recordSchedulerExecution(
                SchedulerName.CLOSING_BET_PRE_SCAN_14,
                SchedulerExecutionStatus.SUCCEEDED
        );
        metrics.recordSchedulerSelected(SchedulerName.CLOSING_BET_PRE_SCAN_14, 3);
        metrics.recordSchedulerNotification(
                SchedulerName.CLOSING_BET_PRE_SCAN_14,
                true
        );
        metrics.recordOrderRequest("BROKER_FAILED");
        metrics.recordBrokerFailure(true);
        metrics.recordOrderRetry("succeeded");
        metrics.recordOrderRetryRecovery("succeeded");
        metrics.recordDiscordNotification("disabled");
        metrics.recordKisReadOnly("currentPrice", "failure");
        metrics.recordAfterHoursLookup("found");
        metrics.recordIntradayBarLookup("found");
        metrics.recordEarlyMarketPerformanceCapture("bars_used");
        metrics.recordEarlyMarketFollowUp("keep");
        metrics.recordEarlyMarketPriceAction("sufficient");
        metrics.recordEarlyMarketReport("success");

        assertThat(counter(
                registry,
                "tradeguard.scheduler.execution.count",
                "status",
                "SUCCEEDED"
        )).isEqualTo(1.0);
        assertThat(counter(
                registry,
                "tradeguard.scheduler.selected.count",
                "schedulerName",
                "CLOSING_BET_PRE_SCAN_14"
        )).isEqualTo(3.0);
        assertThat(counter(
                registry,
                "tradeguard.order.broker_failure.count",
                "retryable",
                "true"
        )).isEqualTo(1.0);
        assertThat(counter(
                registry,
                "tradeguard.notification.discord.count",
                "result",
                "disabled"
        )).isEqualTo(1.0);
        assertThat(counter(
                registry,
                "tradeguard.kis.read_only.count",
                "operation",
                "currentPrice"
        )).isEqualTo(1.0);
        assertThat(counter(
                registry,
                "tradeguard.after_hours.lookup.count",
                "result",
                "found"
        )).isEqualTo(1.0);
        assertThat(counter(
                registry,
                "tradeguard.intraday_bar.lookup.count",
                "result",
                "found"
        )).isEqualTo(1.0);
        assertThat(counter(
                registry,
                "tradeguard.early_market.performance.capture.count",
                "result",
                "bars_used"
        )).isEqualTo(1.0);
        assertThat(counter(
                registry,
                "tradeguard.early_market.follow_up.count",
                "decision",
                "keep"
        )).isEqualTo(1.0);
        assertThat(counter(
                registry,
                "tradeguard.early_market.price_action.count",
                "result",
                "sufficient"
        )).isEqualTo(1.0);
        assertThat(counter(
                registry,
                "tradeguard.early_market.report.count",
                "result",
                "success"
        )).isEqualTo(1.0);

        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .extracting(tag -> tag.getKey() + "=" + tag.getValue())
                .noneMatch(tag -> tag.startsWith("correlationId="))
                .noneMatch(tag -> tag.startsWith("requestCorrelationId="))
                .noneMatch(tag -> tag.contains("005930"))
                .noneMatch(tag -> tag.contains("app-key"))
                .noneMatch(tag -> tag.contains("app-secret"))
                .noneMatch(tag -> tag.contains("webhook"));
    }

    private static double counter(
            SimpleMeterRegistry registry,
            String name,
            String tagKey,
            String tagValue
    ) {
        Meter meter = registry.find(name).tag(tagKey, tagValue).meter();
        assertThat(meter).isNotNull();
        return registry.find(name).tag(tagKey, tagValue).counter().count();
    }
}
