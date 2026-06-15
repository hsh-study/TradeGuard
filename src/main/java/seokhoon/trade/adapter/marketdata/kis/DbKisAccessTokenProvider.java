package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.kis.*;

import java.time.*;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name="tradeguard.kis.token-cache-mode",
        havingValue="DB")
public class DbKisAccessTokenProvider implements KisAccessTokenProvider {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private static final Duration POLL_INTERVAL=Duration.ofMillis(200);

    private final KisTokenClient client;
    private final KisAccessTokenStorePort store;
    private final TokenEncryptionPort encryption;
    private final KisProperties properties;
    private final OperationalMetricsPort metrics;
    private final Clock clock;
    private final String owner;

    @Autowired
    public DbKisAccessTokenProvider(
            KisTokenClient client,
            KisAccessTokenStorePort store,
            TokenEncryptionPort encryption,
            KisProperties properties,
            OperationalMetricsPort metrics
    ) {
        this(client,store,encryption,properties,metrics,Clock.systemUTC(),
                UUID.randomUUID().toString());
    }

    DbKisAccessTokenProvider(
            KisTokenClient client,
            KisAccessTokenStorePort store,
            TokenEncryptionPort encryption,
            KisProperties properties,
            OperationalMetricsPort metrics,
            Clock clock,
            String owner
    ) {
        this.client=client;
        this.store=store;
        this.encryption=encryption;
        this.properties=properties;
        this.metrics=metrics;
        this.clock=clock;
        this.owner=owner;
        properties.validateForRequest();
    }

    @Override
    public String getAccessToken(KisEnvironment environment) {
        Optional<KisAccessToken> current=load(environment);
        if (current.isPresent() && !needsRefresh(current.get())) {
            recordStore("hit");
            return current.get().accessToken();
        }
        recordStore(current.isPresent() ? "hit" : "miss");
        return refreshOrReuse(environment,current).accessToken();
    }

    @Override
    public KisAccessToken getTokenMetadata(KisEnvironment environment) {
        return load(environment).orElseThrow(()->new KisApiException(
                "KIS token is not cached for "+environment));
    }

    @Override
    public Optional<KisAccessToken> findTokenMetadata(
            KisEnvironment environment) {
        return load(environment);
    }

    @Override
    public void refresh(KisEnvironment environment) {
        refreshOrReuse(environment,load(environment));
    }

    @Override
    public void invalidate(KisEnvironment environment) {
        store.clear(environment,clock.instant());
    }

    @Override
    public KisTokenCacheMode cacheMode() {
        return KisTokenCacheMode.DB;
    }

    @Override
    public boolean refreshInProgress(KisEnvironment environment) {
        return store.findByEnvironment(environment)
                .map(StoredKisAccessToken::refreshInProgress)
                .orElse(false);
    }

    private KisAccessToken refreshOrReuse(KisEnvironment environment,
            Optional<KisAccessToken> current) {
        Instant now=clock.instant();
        boolean acquired=store.tryAcquireRefreshLock(environment,owner,now,
                now.minusSeconds(
                        properties.getTokenRefreshLockTimeoutSeconds()));
        if (!acquired) {
            recordStore("lock_busy");
            if (current.isPresent() && isUnexpired(current.get())) {
                return current.get();
            }
            return waitForOtherInstance(environment);
        }
        recordStore("lock_acquired");
        try {
            Optional<KisAccessToken> latest=load(environment);
            if (latest.isPresent() && !needsRefresh(latest.get())) {
                return latest.get();
            }
            try {
                KisAccessToken issued=issue(environment);
                store.save(toStored(issued));
                return issued;
            } catch (RuntimeException exception) {
                Optional<KisAccessToken> fallback=latest.isPresent()
                        ? latest : current;
                if (fallback.isPresent() && isUnexpired(fallback.get())) {
                    return fallback.get();
                }
                recordStore("failure");
                throw exception;
            }
        } finally {
            store.releaseRefreshLock(environment,owner,clock.instant());
        }
    }

    private KisAccessToken waitForOtherInstance(
            KisEnvironment environment) {
        Instant deadline=clock.instant().plusSeconds(
                properties.getTokenRefreshLockWaitSeconds());
        while (clock.instant().isBefore(deadline)) {
            sleep();
            Optional<KisAccessToken> token=load(environment);
            if (token.isPresent() && isUnexpired(token.get())) {
                return token.get();
            }
        }
        throw new KisApiException(
                "KIS token refresh is already in progress");
    }

    private KisAccessToken issue(KisEnvironment environment) {
        try {
            KisAccessToken token=client.issueToken(environment,
                    properties.getAppKey(),properties.getAppSecret());
            metrics.recordKisTokenIssue(environment.name(),"success");
            return token;
        } catch (RuntimeException exception) {
            metrics.recordKisTokenIssue(environment.name(),"failure");
            throw exception;
        }
    }

    private Optional<KisAccessToken> load(KisEnvironment environment) {
        return store.findByEnvironment(environment)
                .filter(StoredKisAccessToken::tokenPresent)
                .map(value->new KisAccessToken(value.environment(),
                        encryption.decrypt(value.encryptedAccessToken()),
                        value.tokenType(),value.expiresAt(),value.issuedAt(),
                        null));
    }

    private StoredKisAccessToken toStored(KisAccessToken token) {
        return new StoredKisAccessToken(token.environment(),
                token.tokenType(),encryption.encrypt(token.accessToken()),
                token.issuedAt(),token.expiresAt(),
                LocalDate.ofInstant(token.issuedAt(),SEOUL),null,null);
    }

    private boolean needsRefresh(KisAccessToken token) {
        Instant now=clock.instant();
        if (!now.isBefore(token.expiresAt().minusSeconds(
                properties.getTokenRefreshBeforeSeconds()))) return true;
        return properties.isTokenDailyRefreshEnabled()
                && !LocalDate.ofInstant(token.issuedAt(),SEOUL).equals(
                        LocalDate.ofInstant(now,SEOUL));
    }

    private boolean isUnexpired(KisAccessToken token) {
        return clock.instant().isBefore(token.expiresAt());
    }

    private void recordStore(String result) {
        metrics.recordKisTokenStore("DB",result);
    }

    private static void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KisApiException(
                    "KIS token refresh wait was interrupted",exception);
        }
    }
}
