package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tradeguard.market-index-provider")
public class MarketIndexProviderProperties {
    private boolean enabled;
    private String type = "KIS";
    private int timeoutSeconds = 10;
    private boolean importAutoRun;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isImportAutoRun() {
        return importAutoRun;
    }

    public void setImportAutoRun(boolean importAutoRun) {
        this.importAutoRun = importAutoRun;
    }
}
