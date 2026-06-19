package seokhoon.trade.config;
import org.springframework.boot.context.properties.ConfigurationProperties;import org.springframework.stereotype.Component;
import java.time.Duration;
@Component @ConfigurationProperties(prefix="tradeguard.consensus-provider")
public class ConsensusProviderProperties {
    private boolean enabled;private String type="CSV";private boolean autoRun;private int timeoutSeconds=10;private int lookbackDays=90;private int maxItemsPerStock=20;
    public boolean isEnabled(){return enabled;}public void setEnabled(boolean v){enabled=v;}public String getType(){return type;}public void setType(String v){type=v;}
    public boolean isAutoRun(){return autoRun;}public void setAutoRun(boolean v){autoRun=v;}public int getTimeoutSeconds(){return timeoutSeconds;}public void setTimeoutSeconds(int v){timeoutSeconds=v;}
    public int getLookbackDays(){return lookbackDays;}public void setLookbackDays(int v){lookbackDays=v;}public int getMaxItemsPerStock(){return maxItemsPerStock;}public void setMaxItemsPerStock(int v){maxItemsPerStock=v;}
    public Duration timeout(){return Duration.ofSeconds(timeoutSeconds);}
}
