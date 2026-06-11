package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

public interface OperationalMetricsPort {
    void recordSchedulerExecution(
            SchedulerName schedulerName,
            SchedulerExecutionStatus status
    );

    void recordSchedulerSelected(SchedulerName schedulerName, int selectedCount);

    void recordSchedulerNotification(SchedulerName schedulerName, boolean sent);

    void recordOrderRequest(String status);

    void recordBrokerFailure(boolean retryable);

    void recordOrderRetry(String result);

    void recordOrderRetryRecovery(String result);

    void recordDiscordNotification(String result);

    void recordKisReadOnly(String operation, String result);

    void recordAfterHoursLookup(String result);

    void recordIntradayBarLookup(String result);

    void recordEarlyMarketPerformanceCapture(String result);

    void recordEarlyMarketFollowUp(String decision);

    void recordEarlyMarketPriceAction(String result);

    void recordEarlyMarketReport(String result);

    void recordEarlyMarketPeriodReport(String result);

    void recordEarlyMarketExperiment(String result);

    void recordEarlyMarketExperimentCompare(String result);

    void recordEarlyMarketBacktest(String result);

    void recordEarlyMarketFollowUpPersist(String result);

    static OperationalMetricsPort noop() {
        return new OperationalMetricsPort() {
            @Override
            public void recordSchedulerExecution(
                    SchedulerName schedulerName,
                    SchedulerExecutionStatus status
            ) {
            }

            @Override
            public void recordSchedulerSelected(
                    SchedulerName schedulerName,
                    int selectedCount
            ) {
            }

            @Override
            public void recordSchedulerNotification(
                    SchedulerName schedulerName,
                    boolean sent
            ) {
            }

            @Override
            public void recordOrderRequest(String status) {
            }

            @Override
            public void recordBrokerFailure(boolean retryable) {
            }

            @Override
            public void recordOrderRetry(String result) {
            }

            @Override
            public void recordOrderRetryRecovery(String result) {
            }

            @Override
            public void recordDiscordNotification(String result) {
            }

            @Override
            public void recordKisReadOnly(String operation, String result) {
            }

            @Override
            public void recordAfterHoursLookup(String result) {
            }

            @Override
            public void recordIntradayBarLookup(String result) {
            }

            @Override
            public void recordEarlyMarketPerformanceCapture(String result) {
            }

            @Override
            public void recordEarlyMarketFollowUp(String decision) {
            }

            @Override
            public void recordEarlyMarketPriceAction(String result) {
            }

            @Override
            public void recordEarlyMarketReport(String result) {
            }

            @Override
            public void recordEarlyMarketPeriodReport(String result) {
            }

            @Override
            public void recordEarlyMarketExperiment(String result) {
            }

            @Override
            public void recordEarlyMarketExperimentCompare(String result) {
            }

            @Override
            public void recordEarlyMarketBacktest(String result) {
            }

            @Override
            public void recordEarlyMarketFollowUpPersist(String result) {
            }
        };
    }
}
