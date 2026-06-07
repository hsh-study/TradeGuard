package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tradeguard.kis")
public class KisProperties {
    private String baseUrl = "https://openapivts.koreainvestment.com:29443";
    private String appKey = "";
    private String appSecret = "";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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

    void validateForRequest() {
        if (!"https://openapivts.koreainvestment.com:29443".equals(baseUrl)) {
            throw new IllegalStateException("Only the KIS virtual investment host is allowed");
        }
        if (appKey == null || appKey.isBlank()) {
            throw new IllegalStateException("KIS app key is not configured");
        }
        if (appSecret == null || appSecret.isBlank()) {
            throw new IllegalStateException("KIS app secret is not configured");
        }
    }
}
