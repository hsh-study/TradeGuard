package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tradeguard.research.news.naver")
public class NaverNewsProperties {
    private boolean providerEnabled;
    private String clientId = "";
    private String clientSecret = "";
    private String apiBaseUrl = "https://openapi.naver.com";
    private String searchPath = "/v1/search/news.json";
    private int requestTimeoutSeconds = 10;
    private int maxDisplay = 20;
    private String sort = "date";
    private int lookbackHours = 24;
    private boolean autoRun;
    private String autoRunCron = "0 20 7,12,16 * * MON-FRI";
    private int queryMaxLength = 100;
    private int maxSymbolsPerRun = 20;
    private int morningNoteTopN = 5;

    public void validateEnabled() {
        if (!providerEnabled) return;
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank())
            throw new IllegalStateException("Naver News credentials are not configured");
        if (requestTimeoutSeconds < 1 || maxDisplay < 1 || maxDisplay > 100
                || queryMaxLength < 1 || maxSymbolsPerRun < 1)
            throw new IllegalStateException("Naver News configuration is invalid");
        if (!"date".equals(sort) && !"sim".equals(sort))
            throw new IllegalStateException("Naver News sort is invalid");
    }

    public boolean isProviderEnabled(){return providerEnabled;} public void setProviderEnabled(boolean v){providerEnabled=v;}
    public String getClientId(){return clientId;} public void setClientId(String v){clientId=v;}
    public String getClientSecret(){return clientSecret;} public void setClientSecret(String v){clientSecret=v;}
    public String getApiBaseUrl(){return apiBaseUrl;} public void setApiBaseUrl(String v){apiBaseUrl=v;}
    public String getSearchPath(){return searchPath;} public void setSearchPath(String v){searchPath=v;}
    public int getRequestTimeoutSeconds(){return requestTimeoutSeconds;} public void setRequestTimeoutSeconds(int v){requestTimeoutSeconds=v;}
    public int getMaxDisplay(){return maxDisplay;} public void setMaxDisplay(int v){maxDisplay=v;}
    public String getSort(){return sort;} public void setSort(String v){sort=v;}
    public int getLookbackHours(){return lookbackHours;} public void setLookbackHours(int v){lookbackHours=v;}
    public boolean isAutoRun(){return autoRun;} public void setAutoRun(boolean v){autoRun=v;}
    public String getAutoRunCron(){return autoRunCron;} public void setAutoRunCron(String v){autoRunCron=v;}
    public int getQueryMaxLength(){return queryMaxLength;} public void setQueryMaxLength(int v){queryMaxLength=v;}
    public int getMaxSymbolsPerRun(){return maxSymbolsPerRun;} public void setMaxSymbolsPerRun(int v){maxSymbolsPerRun=v;}
    public int getMorningNoteTopN(){return morningNoteTopN;} public void setMorningNoteTopN(int v){morningNoteTopN=v;}
}
