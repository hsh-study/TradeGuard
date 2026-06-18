package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="tradeguard.supply-demand.strategy")
public class SupplyDemandStrategyProperties {
    private boolean enabled=true;
    private int strongScore=10;
    private int distributionPenalty=-10;
    private boolean excludeDistribution;
    public boolean isEnabled(){return enabled;}
    public void setEnabled(boolean v){enabled=v;}
    public int getStrongScore(){return strongScore;}
    public void setStrongScore(int v){strongScore=v;}
    public int getDistributionPenalty(){return distributionPenalty;}
    public void setDistributionPenalty(int v){distributionPenalty=v;}
    public boolean isExcludeDistribution(){return excludeDistribution;}
    public void setExcludeDistribution(boolean v){excludeDistribution=v;}
}
