package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@ConfigurationProperties(prefix = "tradeguard.paper-trading-report")
public class PaperTradingReportProperties {
    public enum ClosingBetExitPolicy { NEXT_CLOSE, NEXT_OPEN }

    private boolean enabled = true;
    private boolean autoRun;
    private ClosingBetExitPolicy closingBetExitPolicy = ClosingBetExitPolicy.NEXT_CLOSE;
    private LocalTime earlyMarketEntryTime = LocalTime.of(9, 5);
    private LocalTime earlyMarketExitTime = LocalTime.of(9, 31);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isAutoRun() { return autoRun; }
    public void setAutoRun(boolean autoRun) { this.autoRun = autoRun; }
    public ClosingBetExitPolicy getClosingBetExitPolicy() { return closingBetExitPolicy; }
    public void setClosingBetExitPolicy(ClosingBetExitPolicy value) { this.closingBetExitPolicy = value; }
    public LocalTime getEarlyMarketEntryTime() { return earlyMarketEntryTime; }
    public void setEarlyMarketEntryTime(LocalTime value) { this.earlyMarketEntryTime = value; }
    public LocalTime getEarlyMarketExitTime() { return earlyMarketExitTime; }
    public void setEarlyMarketExitTime(LocalTime value) { this.earlyMarketExitTime = value; }
}
