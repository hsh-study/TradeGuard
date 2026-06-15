package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.kis.KisAccessToken;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.kis.KisTokenCacheMode;

import java.util.Optional;

public interface KisAccessTokenProvider {
    String getAccessToken(KisEnvironment environment);
    KisAccessToken getTokenMetadata(KisEnvironment environment);
    Optional<KisAccessToken> findTokenMetadata(KisEnvironment environment);
    void refresh(KisEnvironment environment);
    void invalidate(KisEnvironment environment);
    KisTokenCacheMode cacheMode();
    boolean refreshInProgress(KisEnvironment environment);
}
