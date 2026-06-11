package seokhoon.trade.adapter.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

@Component
public class MicrometerOperationalMetricsAdapter implements OperationalMetricsPort {
    private final MeterRegistry meterRegistry;

    public MicrometerOperationalMetricsAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordSchedulerExecution(
            SchedulerName schedulerName,
            SchedulerExecutionStatus status
    ) {
        meterRegistry.counter(
                "tradeguard.scheduler.execution.count",
                "schedulerName", schedulerName.name(),
                "status", status.name()
        ).increment();
    }

    @Override
    public void recordSchedulerSelected(SchedulerName schedulerName, int selectedCount) {
        if (selectedCount > 0) {
            meterRegistry.counter(
                    "tradeguard.scheduler.selected.count",
                    "schedulerName", schedulerName.name()
            ).increment(selectedCount);
        }
    }

    @Override
    public void recordSchedulerNotification(SchedulerName schedulerName, boolean sent) {
        meterRegistry.counter(
                "tradeguard.scheduler.notification.sent.count",
                "schedulerName", schedulerName.name(),
                "sent", Boolean.toString(sent)
        ).increment();
    }

    @Override
    public void recordOrderRequest(String status) {
        meterRegistry.counter(
                "tradeguard.order.request.count",
                "status", status
        ).increment();
    }

    @Override
    public void recordBrokerFailure(boolean retryable) {
        meterRegistry.counter(
                "tradeguard.order.broker_failure.count",
                "retryable", Boolean.toString(retryable)
        ).increment();
    }

    @Override
    public void recordOrderRetry(String result) {
        meterRegistry.counter(
                "tradeguard.order.retry.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordOrderRetryRecovery(String result) {
        meterRegistry.counter(
                "tradeguard.order.retry_recovery.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordDiscordNotification(String result) {
        meterRegistry.counter(
                "tradeguard.notification.discord.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordKisReadOnly(String operation, String result) {
        meterRegistry.counter(
                "tradeguard.kis.read_only.count",
                "operation", operation,
                "result", result
        ).increment();
    }

    @Override
    public void recordAfterHoursLookup(String result) {
        meterRegistry.counter(
                "tradeguard.after_hours.lookup.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordIntradayBarLookup(String result) {
        meterRegistry.counter(
                "tradeguard.intraday_bar.lookup.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketPerformanceCapture(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.performance.capture.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketFollowUp(String decision) {
        meterRegistry.counter(
                "tradeguard.early_market.follow_up.count",
                "decision", decision
        ).increment();
    }

    @Override
    public void recordEarlyMarketPriceAction(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.price_action.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketReport(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.report.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketPeriodReport(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.period_report.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketFollowUpPersist(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.follow_up.persist.count",
                "result", result
        ).increment();
    }
}
