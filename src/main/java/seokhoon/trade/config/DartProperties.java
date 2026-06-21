package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.in.ExternalApiConfigurationUseCase;

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
    private boolean corpCodeImportEnabled = false;
    private String corpCodeZipUrl = "";
    private int corpCodeImportTimeoutSeconds = 20;
    private boolean corpCodeImportAutoMatchListedOnly = true;
    private ExternalApiConfigurationUseCase databaseConfigurations;

    @Autowired(required = false)
    public void setDatabaseConfigurations(ExternalApiConfigurationUseCase value) { this.databaseConfigurations=value; }

    public boolean isProviderEnabled() {
        return databaseConfigurations == null ? providerEnabled
                : databaseConfigurations.dartConfig().map(ExternalApiConfigurationUseCase.DartConfigView::active).orElse(providerEnabled);
    }

    public void setProviderEnabled(boolean providerEnabled) {
        this.providerEnabled = providerEnabled;
    }

    public String getApiBaseUrl() {
        return databaseConfigurations == null ? apiBaseUrl
                : databaseConfigurations.dartCredentials().map(ExternalApiConfigurationUseCase.DartCredentials::baseUrl).orElse(apiBaseUrl);
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getApiKey() {
        return databaseConfigurations == null ? apiKey
                : databaseConfigurations.dartCredentials().map(ExternalApiConfigurationUseCase.DartCredentials::apiKey).orElse(apiKey);
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

    public boolean isCorpCodeImportEnabled() {
        return corpCodeImportEnabled;
    }

    public void setCorpCodeImportEnabled(boolean corpCodeImportEnabled) {
        this.corpCodeImportEnabled = corpCodeImportEnabled;
    }

    public String getCorpCodeZipUrl() {
        return corpCodeZipUrl;
    }

    public void setCorpCodeZipUrl(String corpCodeZipUrl) {
        this.corpCodeZipUrl = corpCodeZipUrl;
    }

    public int getCorpCodeImportTimeoutSeconds() {
        return corpCodeImportTimeoutSeconds;
    }

    public void setCorpCodeImportTimeoutSeconds(int corpCodeImportTimeoutSeconds) {
        this.corpCodeImportTimeoutSeconds = corpCodeImportTimeoutSeconds;
    }

    public boolean isCorpCodeImportAutoMatchListedOnly() {
        return corpCodeImportAutoMatchListedOnly;
    }

    public void setCorpCodeImportAutoMatchListedOnly(boolean corpCodeImportAutoMatchListedOnly) {
        this.corpCodeImportAutoMatchListedOnly = corpCodeImportAutoMatchListedOnly;
    }

    public Duration requestTimeout() {
        return Duration.ofSeconds(requestTimeoutSeconds);
    }

    public Duration corpCodeImportTimeout() {
        return Duration.ofSeconds(corpCodeImportTimeoutSeconds);
    }

    public void validateProviderRequest() {
        if (!providerEnabled) {
            throw new DartProviderException("DART provider is disabled");
        }
        if (getApiBaseUrl() == null || getApiBaseUrl().isBlank()) {
            throw new DartProviderException("DART API base URL is not configured");
        }
        if (getApiKey() == null || getApiKey().isBlank()) {
            throw new DartProviderException("DART API key is not configured");
        }
        if (requestTimeoutSeconds <= 0) {
            throw new DartProviderException("DART request timeout must be positive");
        }
    }

    public void validateCorpCodeImportRequest() {
        if (!corpCodeImportEnabled) {
            throw new DartProviderException("DART corp code import is disabled");
        }
        if (corpCodeZipUrl == null || corpCodeZipUrl.isBlank()) {
            throw new DartProviderException("DART corp code zip URL is not configured");
        }
        if (corpCodeImportTimeoutSeconds <= 0) {
            throw new DartProviderException("DART corp code import timeout must be positive");
        }
    }
}
