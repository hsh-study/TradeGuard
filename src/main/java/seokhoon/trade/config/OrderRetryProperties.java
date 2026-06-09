package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tradeguard.order")
public class OrderRetryProperties {
    private long retryStuckThresholdMinutes = 5;

    public long getRetryStuckThresholdMinutes() {
        return retryStuckThresholdMinutes;
    }

    public void setRetryStuckThresholdMinutes(long retryStuckThresholdMinutes) {
        if (retryStuckThresholdMinutes < 1) {
            throw new IllegalArgumentException(
                    "retry-stuck-threshold-minutes must be at least 1"
            );
        }
        this.retryStuckThresholdMinutes = retryStuckThresholdMinutes;
    }
}
