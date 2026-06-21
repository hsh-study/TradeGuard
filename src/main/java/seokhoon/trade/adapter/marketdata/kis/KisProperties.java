package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.in.ExternalApiConfigurationUseCase;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
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
    public static final String REAL_WEBSOCKET_URL =
            "ws://ops.koreainvestment.com:21000/tryitout";
    public static final String DEMO_WEBSOCKET_URL =
            "ws://ops.koreainvestment.com:31000/tryitout";

    private KisEnvironment environment = KisEnvironment.DEMO;
    private String realBaseUrl = REAL_BASE_URL;
    private String demoBaseUrl = DEMO_BASE_URL;
    private String realWebsocketUrl = REAL_WEBSOCKET_URL;
    private String demoWebsocketUrl = DEMO_WEBSOCKET_URL;
    private String websocketApprovalPath = "/oauth2/Approval";
    private String tokenPath = "/oauth2/tokenP";
    private String appKey = "";
    private String appSecret = "";
    private int tokenRefreshBeforeSeconds = 600;
    private boolean tokenDailyRefreshEnabled = true;
    private LocalTime tokenIssueTimeKst = LocalTime.of(7, 30);
    private KisTokenCacheMode tokenCacheMode = KisTokenCacheMode.MEMORY;
    private String baseUrlOverride = "";
    private String tokenEncryptionKey = "";
    private int tokenRefreshLockTimeoutSeconds = 120;
    private int tokenRefreshLockWaitSeconds = 10;
    private ExternalApiConfigurationUseCase databaseConfigurations;
    private TradingAccountManagementUseCase tradingAccounts;

    @Autowired(required = false)
    public void setDatabaseConfigurations(ExternalApiConfigurationUseCase value) {
        this.databaseConfigurations = value;
    }
    @Autowired(required = false)
    public void setTradingAccounts(TradingAccountManagementUseCase value) { this.tradingAccounts=value; }

    public String getBaseUrl() {
        return baseUrl(getEnvironment());
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrlOverride = baseUrl;
    }

    public String getAppKey() {
        return appKey(getEnvironment());
    }

    public String appKey(KisEnvironment requestedEnvironment) {
        return configuration(requestedEnvironment).map(ExternalApiConfigurationUseCase.KisCredentials::appKey).orElse(appKey);
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret(getEnvironment());
    }

    public String appSecret(KisEnvironment requestedEnvironment) {
        return configuration(requestedEnvironment).map(ExternalApiConfigurationUseCase.KisCredentials::appSecret).orElse(appSecret);
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public void validateForRequest() {
        validateForRequest(getEnvironment());
    }

    public void validateForRequest(KisEnvironment requestedEnvironment) {
        if (appKey(requestedEnvironment) == null || appKey(requestedEnvironment).isBlank()) {
            throw new IllegalStateException("KIS app key is not configured");
        }
        if (appSecret(requestedEnvironment) == null || appSecret(requestedEnvironment).isBlank()) {
            throw new IllegalStateException("KIS app secret is not configured");
        }
        if (tokenRefreshBeforeSeconds < 0) {
            throw new IllegalStateException(
                    "tokenRefreshBeforeSeconds must be non-negative");
        }
        if (tokenCacheMode == KisTokenCacheMode.DB
                && (tokenEncryptionKey == null
                || tokenEncryptionKey.isBlank())) {
            throw new IllegalStateException(
                    "KIS token encryption key is required for DB cache");
        }
        if (tokenRefreshLockTimeoutSeconds <= 0
                || tokenRefreshLockWaitSeconds < 0) {
            throw new IllegalStateException(
                    "KIS token refresh lock settings are invalid");
        }
        baseUrl(requestedEnvironment);
    }

    public String baseUrl(KisEnvironment requestedEnvironment) {
        if (baseUrlOverride != null && !baseUrlOverride.isBlank()) {
            return baseUrlOverride;
        }
        var configured = configuration(requestedEnvironment);
        if (configured.isPresent()) return configured.get().baseUrl();
        return requestedEnvironment == KisEnvironment.REAL
                ? realBaseUrl : demoBaseUrl;
    }

    public String websocketUrl(KisEnvironment requestedEnvironment) {
        return requestedEnvironment == KisEnvironment.REAL
                ? realWebsocketUrl : demoWebsocketUrl;
    }

    private java.util.Optional<ExternalApiConfigurationUseCase.KisCredentials> configuration(KisEnvironment environment) {
        return databaseConfigurations == null ? java.util.Optional.empty()
                : databaseConfigurations.kisCredentials(environment);
    }

    public KisEnvironment getEnvironment() { return tradingAccounts == null ? environment
            : tradingAccounts.primaryCredentials().map(TradingAccountManagementUseCase.AccountCredentials::environment).orElse(environment); }
    public void setEnvironment(KisEnvironment value) { environment = value; }
    public String getRealBaseUrl() { return realBaseUrl; }
    public void setRealBaseUrl(String value) { realBaseUrl = value; }
    public String getDemoBaseUrl() { return demoBaseUrl; }
    public void setDemoBaseUrl(String value) { demoBaseUrl = value; }
    public String getRealWebsocketUrl() { return realWebsocketUrl; }
    public void setRealWebsocketUrl(String value) { realWebsocketUrl = value; }
    public String getDemoWebsocketUrl() { return demoWebsocketUrl; }
    public void setDemoWebsocketUrl(String value) { demoWebsocketUrl = value; }
    public String getWebsocketApprovalPath() { return websocketApprovalPath; }
    public void setWebsocketApprovalPath(String value) { websocketApprovalPath = value; }
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
    public String getTokenEncryptionKey() { return tokenEncryptionKey; }
    public void setTokenEncryptionKey(String value) { tokenEncryptionKey = value; }
    public int getTokenRefreshLockTimeoutSeconds() { return tokenRefreshLockTimeoutSeconds; }
    public void setTokenRefreshLockTimeoutSeconds(int value) { tokenRefreshLockTimeoutSeconds = value; }
    public int getTokenRefreshLockWaitSeconds() { return tokenRefreshLockWaitSeconds; }
    public void setTokenRefreshLockWaitSeconds(int value) { tokenRefreshLockWaitSeconds = value; }
    public String tokenRefreshCron() {
        return "0 " + tokenIssueTimeKst.getMinute()
                + " " + tokenIssueTimeKst.getHour() + " * * *";
    }

    @Override
    public KisEnvironment readOnlyEnvironment() {
        return getEnvironment();
    }

    @Override
    public boolean credentialsConfigured() {
        return appKey(getEnvironment()) != null && !appKey(getEnvironment()).isBlank()
                && appSecret(getEnvironment()) != null && !appSecret(getEnvironment()).isBlank();
    }

    @Override
    public int tokenRefreshBeforeSeconds() {
        return tokenRefreshBeforeSeconds;
    }

    @Override
    public KisTokenCacheMode tokenCacheMode() {
        return tokenCacheMode;
    }

    @Override
    public boolean tokenEncryptionConfigured() {
        return tokenEncryptionKey != null && !tokenEncryptionKey.isBlank();
    }

    @Override
    public boolean tokenDailyRefreshEnabled() {
        return tokenDailyRefreshEnabled;
    }
}
