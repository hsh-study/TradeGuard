package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.kis.*;

import java.time.Instant;
import java.util.Optional;

public interface KisAccessTokenStorePort {
    Optional<StoredKisAccessToken> findByEnvironment(
            KisEnvironment environment);
    StoredKisAccessToken save(StoredKisAccessToken token);
    boolean tryAcquireRefreshLock(KisEnvironment environment, String owner,
            Instant now, Instant staleBefore);
    void releaseRefreshLock(KisEnvironment environment, String owner,
            Instant now);
    void clear(KisEnvironment environment, Instant now);
}
