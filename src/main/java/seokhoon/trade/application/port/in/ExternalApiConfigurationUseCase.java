package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.kis.KisEnvironment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExternalApiConfigurationUseCase {
    List<KisConfigView> kisConfigs();
    KisConfigView saveKis(KisConfigCommand command);
    Optional<KisCredentials> kisCredentials(KisEnvironment environment);
    Optional<DartCredentials> dartCredentials();
    DartConfigView saveDart(DartConfigCommand command);
    Optional<DartConfigView> dartConfig();

    record KisConfigCommand(KisEnvironment environment, String appKey, String appSecret,
            String baseUrl, boolean active) {}
    record KisConfigView(KisEnvironment environment, boolean appKeyConfigured,
            boolean appSecretConfigured, String maskedAppKey, String baseUrl,
            boolean active, Instant updatedAt) {}
    record KisCredentials(KisEnvironment environment, String appKey, String appSecret,
            String baseUrl, boolean active) {}
    record DartConfigCommand(String apiKey, String baseUrl, boolean active) {}
    record DartConfigView(boolean apiKeyConfigured, String maskedApiKey, String baseUrl,
            boolean active, Instant updatedAt) {}
    record DartCredentials(String apiKey, String baseUrl, boolean active) {}
}
