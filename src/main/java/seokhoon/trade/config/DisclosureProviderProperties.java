package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import seokhoon.trade.domain.research.DisclosureProvider;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "tradeguard.disclosure-provider")
public class DisclosureProviderProperties {
    private boolean enabled = false;
    private DisclosureProvider type = DisclosureProvider.DART;
    private int timeoutSeconds = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DisclosureProvider getType() {
        return type;
    }

    public void setType(DisclosureProvider type) {
        this.type = type;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Duration timeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }
}
