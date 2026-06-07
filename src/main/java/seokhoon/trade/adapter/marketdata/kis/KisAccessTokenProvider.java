package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
class KisAccessTokenProvider {
    private static final Duration EXPIRY_SAFETY_MARGIN = Duration.ofMinutes(1);
    private final KisHttpClient httpClient;
    private final KisProperties properties;
    private final Clock clock;
    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    @Autowired
    KisAccessTokenProvider(KisHttpClient httpClient, KisProperties properties) {
        this(httpClient, properties, Clock.systemUTC());
    }

    KisAccessTokenProvider(KisHttpClient httpClient, KisProperties properties, Clock clock) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.clock = clock;
    }

    synchronized String getAccessToken() {
        properties.validateForRequest();
        if (accessToken != null && clock.instant().isBefore(expiresAt.minus(EXPIRY_SAFETY_MARGIN))) {
            return accessToken;
        }

        URI uri = URI.create(properties.getBaseUrl() + "/oauth2/tokenP");
        KisHttpResponse response = httpClient.postJson(uri, Map.of(), Map.of(
                "grant_type", "client_credentials",
                "appkey", properties.getAppKey(),
                "appsecret", properties.getAppSecret()
        ));
        if (response.statusCode() != 200) {
            throw new KisApiException("KIS token request failed with HTTP " + response.statusCode());
        }

        JsonNode tokenNode = response.body().path("access_token");
        if (!tokenNode.isTextual() || tokenNode.textValue().isBlank()) {
            throw new KisApiException("KIS token response did not contain an access token");
        }
        long expiresIn = response.body().path("expires_in").asLong(0);
        if (expiresIn <= 0) {
            throw new KisApiException("KIS token response did not contain a valid expiry");
        }

        accessToken = tokenNode.textValue();
        expiresAt = clock.instant().plusSeconds(expiresIn);
        return accessToken;
    }
}
