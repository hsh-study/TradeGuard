package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import seokhoon.trade.domain.research.DisclosureProvider;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "tradeguard.disclosure-actual-provider")
public class DisclosureActualProviderProperties {
    private boolean enabled;
    private DisclosureProvider type = DisclosureProvider.DART;
    private int timeoutSeconds = 10;
    private boolean autoRun;
    private int lookbackDays = 7;
    private int maxItemsPerStock = 20;
    private int rateLimitPerMinute = 30;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public DisclosureProvider getType() { return type; }
    public void setType(DisclosureProvider type) { this.type = type; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int value) { this.timeoutSeconds = value; }
    public boolean isAutoRun() { return autoRun; }
    public void setAutoRun(boolean autoRun) { this.autoRun = autoRun; }
    public int getLookbackDays() { return lookbackDays; }
    public void setLookbackDays(int value) { this.lookbackDays = value; }
    public int getMaxItemsPerStock() { return maxItemsPerStock; }
    public void setMaxItemsPerStock(int value) { this.maxItemsPerStock = value; }
    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(int value) { this.rateLimitPerMinute = value; }
    public Duration timeout() { return Duration.ofSeconds(timeoutSeconds); }

    public void validateRequest() {
        if (!enabled) throw new IllegalStateException("disclosure actual provider is disabled");
        if (type != DisclosureProvider.DART) throw new IllegalStateException("unsupported disclosure actual provider type");
        if (timeoutSeconds <= 0 || lookbackDays < 0 || maxItemsPerStock <= 0
                || maxItemsPerStock > 100 || rateLimitPerMinute <= 0) {
            throw new IllegalStateException("invalid disclosure actual provider configuration");
        }
    }
}
