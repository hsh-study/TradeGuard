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
}
