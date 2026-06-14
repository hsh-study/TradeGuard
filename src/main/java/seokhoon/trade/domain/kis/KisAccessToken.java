package seokhoon.trade.domain.kis;

import java.time.Instant;
import java.util.Objects;

public final class KisAccessToken {
    private final KisEnvironment environment;
    private final String accessToken;
    private final String tokenType;
    private final Instant expiresAt;
    private final Instant issuedAt;
    private final String sanitizedTokenId;

    public KisAccessToken(
            KisEnvironment environment,
            String accessToken,
            String tokenType,
            Instant expiresAt,
            Instant issuedAt,
            String sanitizedTokenId
    ) {
        this.environment = Objects.requireNonNull(environment, "environment");
        this.accessToken = requireText(accessToken, "accessToken");
        this.tokenType = requireText(tokenType, "tokenType");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.sanitizedTokenId = sanitizedTokenId;
    }

    public KisEnvironment environment() { return environment; }
    public String accessToken() { return accessToken; }
    public String tokenType() { return tokenType; }
    public Instant expiresAt() { return expiresAt; }
    public Instant issuedAt() { return issuedAt; }
    public String sanitizedTokenId() { return sanitizedTokenId; }

    @Override
    public String toString() {
        return "KisAccessToken[environment=" + environment
                + ", tokenType=" + tokenType
                + ", expiresAt=" + expiresAt
                + ", issuedAt=" + issuedAt
                + ", sanitizedTokenId=" + sanitizedTokenId + "]";
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
