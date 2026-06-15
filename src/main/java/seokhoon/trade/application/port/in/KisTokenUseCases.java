package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.kis.KisTokenCacheMode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class KisTokenUseCases {
    private KisTokenUseCases() {}

    public interface ManageKisTokenUseCase {
        List<KisTokenStatus> statuses();
        KisTokenStatus refresh(KisEnvironment environment);
        KisTokenStatus invalidate(KisEnvironment environment);
    }

    public record KisTokenStatus(
            KisTokenCacheMode cacheMode,
            KisEnvironment environment,
            boolean tokenPresent,
            Instant expiresAt,
            long secondsToExpire,
            LocalDate dailyIssuedDate,
            boolean refreshInProgress
    ) {}
}
