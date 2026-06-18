package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import seokhoon.trade.domain.market.KisInvestorFlowAmountUnit;

@Component
@ConfigurationProperties(prefix="tradeguard.investor-flow")
public class InvestorFlowProperties {
    private boolean providerEnabled;
    private String providerType="KIS";
    private int providerTimeoutSeconds=10;
    private boolean importAutoRun;
    private int lookbackDays=20;
    private KisInvestorFlowAmountUnit kisAmountUnit=KisInvestorFlowAmountUnit.UNVERIFIED;
    public boolean isProviderEnabled(){return providerEnabled;}
    public void setProviderEnabled(boolean v){providerEnabled=v;}
    public String getProviderType(){return providerType;}
    public void setProviderType(String v){providerType=v;}
    public int getProviderTimeoutSeconds(){return providerTimeoutSeconds;}
    public void setProviderTimeoutSeconds(int v){providerTimeoutSeconds=v;}
    public boolean isImportAutoRun(){return importAutoRun;}
    public void setImportAutoRun(boolean v){importAutoRun=v;}
    public int getLookbackDays(){return lookbackDays;}
    public void setLookbackDays(int v){lookbackDays=v;}
    public KisInvestorFlowAmountUnit getKisAmountUnit(){return kisAmountUnit;}
    public void setKisAmountUnit(KisInvestorFlowAmountUnit v){kisAmountUnit=v;}
}
