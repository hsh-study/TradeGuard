package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.application.port.out.KisTokenClient;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.kis.KisAccessToken;
import seokhoon.trade.domain.kis.KisEnvironment;

import java.time.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
@ConditionalOnProperty(name="tradeguard.kis.token-cache-mode",
        havingValue="MEMORY",matchIfMissing=true)
public class InMemoryKisAccessTokenProvider
        implements KisAccessTokenProvider {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final KisTokenClient tokenClient;
    private final KisProperties properties;
    private final OperationalMetricsPort metrics;
    private final Clock clock;
    private final Map<KisEnvironment,KisAccessToken> tokens =
            new ConcurrentHashMap<>();
    private final Map<KisEnvironment,ReentrantLock> locks =
            new ConcurrentHashMap<>();

    @Autowired
    public InMemoryKisAccessTokenProvider(
            KisTokenClient tokenClient,
            KisProperties properties,
            OperationalMetricsPort metrics
    ) {
        this(tokenClient,properties,metrics,Clock.systemUTC());
    }

    InMemoryKisAccessTokenProvider(
            KisTokenClient tokenClient,
            KisProperties properties,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.tokenClient=tokenClient;
        this.properties=properties;
        this.metrics=metrics;
        this.clock=clock;
        for (KisEnvironment environment : KisEnvironment.values()) {
            locks.put(environment,new ReentrantLock());
        }
    }

    @Override
    public String getAccessToken(KisEnvironment environment) {
        KisAccessToken current = tokens.get(environment);
        if (!needsRefresh(current)) {
            metrics.recordKisTokenCache(environment.name(),"hit");
            return current.accessToken();
        }
        metrics.recordKisTokenCache(environment.name(),
                current == null ? "miss" : "refresh");
        ReentrantLock lock=locks.get(environment);
        lock.lock();
        try {
            current=tokens.get(environment);
            if (!needsRefresh(current)) return current.accessToken();
            try {
                KisAccessToken issued=issue(environment);
                tokens.put(environment,issued);
                return issued.accessToken();
            } catch (RuntimeException exception) {
                if (current != null
                        && clock.instant().isBefore(current.expiresAt())) {
                    return current.accessToken();
                }
                throw exception;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public KisAccessToken getTokenMetadata(KisEnvironment environment) {
        KisAccessToken token=tokens.get(environment);
        if (token == null) {
            throw new KisApiException(
                    "KIS token is not cached for " + environment);
        }
        return token;
    }

    @Override
    public Optional<KisAccessToken> findTokenMetadata(
            KisEnvironment environment) {
        return Optional.ofNullable(tokens.get(environment));
    }

    @Override
    public void refresh(KisEnvironment environment) {
        ReentrantLock lock=locks.get(environment);
        lock.lock();
        try {
            tokens.put(environment,issue(environment));
            metrics.recordKisTokenCache(environment.name(),"refresh");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void invalidate(KisEnvironment environment) {
        tokens.remove(environment);
    }

    @Override
    public seokhoon.trade.domain.kis.KisTokenCacheMode cacheMode() {
        return seokhoon.trade.domain.kis.KisTokenCacheMode.MEMORY;
    }

    @Override
    public boolean refreshInProgress(KisEnvironment environment) {
        return locks.get(environment).isLocked();
    }

    private KisAccessToken issue(KisEnvironment environment) {
        properties.validateForRequest(environment);
        try {
            KisAccessToken token=tokenClient.issueToken(environment,
                    properties.getAppKey(),properties.getAppSecret());
            metrics.recordKisTokenIssue(environment.name(),"success");
            return token;
        } catch (RuntimeException exception) {
            metrics.recordKisTokenIssue(environment.name(),"failure");
            throw exception;
        }
    }

    private boolean needsRefresh(KisAccessToken token) {
        if (token == null) return true;
        Instant now=clock.instant();
        if (!now.isBefore(token.expiresAt().minusSeconds(
                properties.getTokenRefreshBeforeSeconds()))) {
            return true;
        }
        return properties.isTokenDailyRefreshEnabled()
                && !LocalDate.ofInstant(token.issuedAt(),SEOUL).equals(
                        LocalDate.ofInstant(now,SEOUL));
    }
}
