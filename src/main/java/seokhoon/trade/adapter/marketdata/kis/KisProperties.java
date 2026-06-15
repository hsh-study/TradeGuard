package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.kis.KisTokenCacheMode;
import seokhoon.trade.application.port.out.KisConfigurationPort;

import java.time.LocalTime;

@Component
@ConfigurationProperties(prefix = "tradeguard.kis")
public class KisProperties implements KisConfigurationPort {
    public static final String REAL_BASE_URL =
            "https://openapi.koreainvestment.com:9443";
    public static final String DEMO_BASE_URL =
            "https://openapivts.koreainvestment.com:29443";

    private KisEnvironment environment = KisEnvironment.DEMO;
    private String realBaseUrl = REAL_BASE_URL;
    private String demoBaseUrl = DEMO_BASE_URL;
    private String tokenPath = "/oauth2/tokenP";
    private String appKey = "";
    private String appSecret = "";
    private int tokenRefreshBeforeSeconds = 600;
    private boolean tokenDailyRefreshEnabled = true;
    private LocalTime tokenIssueTimeKst = LocalTime.of(7, 30);
    private KisTokenCacheMode tokenCacheMode = KisTokenCacheMode.MEMORY;
    private String baseUrlOverride = "";

    public String getBaseUrl() {
        return baseUrl(environment);
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrlOverride = baseUrl;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public void validateForRequest() {
        validateForRequest(environment);
    }

    public void validateForRequest(KisEnvironment requestedEnvironment) {
        if (appKey == null || appKey.isBlank()) {
            throw new IllegalStateException("KIS app key is not configured");
        }
        if (appSecret == null || appSecret.isBlank()) {
            throw new IllegalStateException("KIS app secret is not configured");
        }
        if (tokenRefreshBeforeSeconds < 0) {
            throw new IllegalStateException(
                    "tokenRefreshBeforeSeconds must be non-negative");
        }
        if (tokenCacheMode != KisTokenCacheMode.MEMORY) {
            throw new IllegalStateException(
                    "Only MEMORY KIS token cache is currently supported");
        }
        baseUrl(requestedEnvironment);
    }

    public String baseUrl(KisEnvironment requestedEnvironment) {
        if (baseUrlOverride != null && !baseUrlOverride.isBlank()) {
            return baseUrlOverride;
        }
        return requestedEnvironment == KisEnvironment.REAL
                ? realBaseUrl : demoBaseUrl;
    }

    public KisEnvironment getEnvironment() { return environment; }
    public void setEnvironment(KisEnvironment value) { environment = value; }
    public String getRealBaseUrl() { return realBaseUrl; }
    public void setRealBaseUrl(String value) { realBaseUrl = value; }
    public String getDemoBaseUrl() { return demoBaseUrl; }
    public void setDemoBaseUrl(String value) { demoBaseUrl = value; }
    public String getTokenPath() { return tokenPath; }
    public void setTokenPath(String value) { tokenPath = value; }
    public int getTokenRefreshBeforeSeconds() { return tokenRefreshBeforeSeconds; }
    public void setTokenRefreshBeforeSeconds(int value) { tokenRefreshBeforeSeconds = value; }
    public boolean isTokenDailyRefreshEnabled() { return tokenDailyRefreshEnabled; }
    public void setTokenDailyRefreshEnabled(boolean value) { tokenDailyRefreshEnabled = value; }
    public LocalTime getTokenIssueTimeKst() { return tokenIssueTimeKst; }
    public void setTokenIssueTimeKst(LocalTime value) { tokenIssueTimeKst = value; }
    public KisTokenCacheMode getTokenCacheMode() { return tokenCacheMode; }
    public void setTokenCacheMode(KisTokenCacheMode value) { tokenCacheMode = value; }
    public String getBaseUrlOverride() { return baseUrlOverride; }
    public void setBaseUrlOverride(String value) { baseUrlOverride = value; }
    public String tokenRefreshCron() {
        return "0 " + tokenIssueTimeKst.getMinute()
                + " " + tokenIssueTimeKst.getHour() + " * * *";
    }

    @Override
    public KisEnvironment readOnlyEnvironment() {
        return environment;
    }

    @Override
    public boolean credentialsConfigured() {
        return appKey != null && !appKey.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }

    @Override
    public int tokenRefreshBeforeSeconds() {
        return tokenRefreshBeforeSeconds;
    }
}
