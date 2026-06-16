package seokhoon.trade.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "tradeguard.earnings.strategy")
public class EarningsStrategyProperties {
    private boolean enabled = true;
    @Min(0)
    private int strongScore = 10;
    @Max(0)
    private int weakPenalty = -10;
    private boolean excludeWeak = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getStrongScore() {
        return strongScore;
    }

    public void setStrongScore(int strongScore) {
        this.strongScore = strongScore;
    }

    public int getWeakPenalty() {
        return weakPenalty;
    }

    public void setWeakPenalty(int weakPenalty) {
        this.weakPenalty = weakPenalty;
    }

    public boolean isExcludeWeak() {
        return excludeWeak;
    }

    public void setExcludeWeak(boolean excludeWeak) {
        this.excludeWeak = excludeWeak;
    }
}
