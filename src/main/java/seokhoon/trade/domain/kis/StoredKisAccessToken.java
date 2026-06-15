package seokhoon.trade.domain.kis;

import java.time.Instant;
import java.time.LocalDate;

public record StoredKisAccessToken(
        KisEnvironment environment,
        String tokenType,
        String encryptedAccessToken,
        Instant issuedAt,
        Instant expiresAt,
        LocalDate dailyIssuedDate,
        Instant refreshStartedAt,
        String refreshOwner
) {
    public boolean tokenPresent() {
        return encryptedAccessToken != null
                && !encryptedAccessToken.isBlank()
                && expiresAt != null;
    }

    public boolean refreshInProgress() {
        return refreshStartedAt != null && refreshOwner != null;
    }
}
