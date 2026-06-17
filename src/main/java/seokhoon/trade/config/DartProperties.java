package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "tradeguard.dart")
public class DartProperties {
    private boolean providerEnabled = false;
    private String apiBaseUrl = "";
    private String apiKey = "";
    private int requestTimeoutSeconds = 10;
    private boolean importAutoAnalyze = true;
    private int importLookbackQuarters = 8;

    public boolean isProviderEnabled() {
        return providerEnabled;
    }

    public void setProviderEnabled(boolean providerEnabled) {
        this.providerEnabled = providerEnabled;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public boolean isImportAutoAnalyze() {
        return importAutoAnalyze;
    }

    public void setImportAutoAnalyze(boolean importAutoAnalyze) {
        this.importAutoAnalyze = importAutoAnalyze;
    }

    public int getImportLookbackQuarters() {
        return importLookbackQuarters;
    }

    public void setImportLookbackQuarters(int importLookbackQuarters) {
        this.importLookbackQuarters = importLookbackQuarters;
    }

    public Duration requestTimeout() {
        return Duration.ofSeconds(requestTimeoutSeconds);
    }

    public void validateProviderRequest() {
        if (!providerEnabled) {
            throw new DartProviderException("DART provider is disabled");
        }
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            throw new DartProviderException("DART API base URL is not configured");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new DartProviderException("DART API key is not configured");
        }
        if (requestTimeoutSeconds <= 0) {
            throw new DartProviderException("DART request timeout must be positive");
        }
    }
}
