package seokhoon.trade.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "tradeguard.indicator.warmup")
public class IndicatorWarmUpProperties {
    private boolean enabled = true;
    @Min(60)
    private int lookbackTradingDays = 120;
    @Min(60)
    private int minRequiredDaysForMa60 = 60;
    private boolean failStrategyWhenInsufficient;
    @Min(1)
    private int maxSymbolsPerRun = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLookbackTradingDays() {
        return lookbackTradingDays;
    }

    public void setLookbackTradingDays(int lookbackTradingDays) {
        this.lookbackTradingDays = lookbackTradingDays;
    }

    public int getMinRequiredDaysForMa60() {
        return minRequiredDaysForMa60;
    }

    public void setMinRequiredDaysForMa60(int minRequiredDaysForMa60) {
        this.minRequiredDaysForMa60 = minRequiredDaysForMa60;
    }

    public boolean isFailStrategyWhenInsufficient() {
        return failStrategyWhenInsufficient;
    }

    public void setFailStrategyWhenInsufficient(
            boolean failStrategyWhenInsufficient
    ) {
        this.failStrategyWhenInsufficient = failStrategyWhenInsufficient;
    }

    public int getMaxSymbolsPerRun() {
        return maxSymbolsPerRun;
    }

    public void setMaxSymbolsPerRun(int maxSymbolsPerRun) {
        this.maxSymbolsPerRun = maxSymbolsPerRun;
    }
}
