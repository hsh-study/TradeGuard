package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.KisTokenClient;
import seokhoon.trade.domain.kis.KisAccessToken;
import seokhoon.trade.domain.kis.KisEnvironment;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Map;

@Component
public class KisOAuthTokenClient implements KisTokenClient {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KIS_EXPIRY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KisHttpClient httpClient;
    private final KisProperties properties;
    private final Clock clock;

    @Autowired
    public KisOAuthTokenClient(
            KisHttpClient httpClient,
            KisProperties properties
    ) {
        this(httpClient, properties, Clock.systemUTC());
    }

    KisOAuthTokenClient(
            KisHttpClient httpClient,
            KisProperties properties,
            Clock clock
    ) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public KisAccessToken issueToken(
            KisEnvironment environment,
            String appKey,
            String appSecret
    ) {
        properties.validateForRequest(environment);
        KisHttpResponse response = httpClient.postJson(
                URI.create(properties.baseUrl(environment)
                        + properties.getTokenPath()),
                Map.of(),
                Map.of(
                        "grant_type", "client_credentials",
                        "appkey", appKey,
                        "appsecret", appSecret
                )
        );
        if (response.statusCode() != 200) {
            throw new KisApiException(
                    "KIS token request failed with HTTP "
                            + response.statusCode());
        }
        JsonNode body = response.body();
        String accessToken = requiredText(body, "access_token");
        String tokenType = body.path("token_type").asText("Bearer");
        Instant issuedAt = clock.instant();
        Instant expiresAt = expiry(body, issuedAt);
        return new KisAccessToken(
                environment,
                accessToken,
                tokenType,
                expiresAt,
                issuedAt,
                sanitizedId(accessToken)
        );
    }

    private static Instant expiry(JsonNode body, Instant issuedAt) {
        long expiresIn = body.path("expires_in").asLong(0);
        if (expiresIn > 0) {
            return issuedAt.plusSeconds(expiresIn);
        }
        String value = body.path("access_token_token_expired").asText("");
        if (!value.isBlank()) {
            try {
                return LocalDateTime.parse(value, KIS_EXPIRY)
                        .atZone(SEOUL)
                        .toInstant();
            } catch (DateTimeParseException exception) {
                throw new KisApiException(
                        "KIS token response contained invalid expiry");
            }
        }
        throw new KisApiException(
                "KIS token response did not contain a valid expiry");
    }

    private static String requiredText(JsonNode body, String field) {
        String value = body.path(field).asText("");
        if (value.isBlank()) {
            String code = body.path("error_code").asText(
                    body.path("msg_cd").asText("UNKNOWN"));
            throw new KisApiException(
                    "KIS token response missing " + field + ": " + code);
        }
        return value;
    }

    private static String sanitizedId(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
