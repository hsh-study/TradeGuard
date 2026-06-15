package seokhoon.trade.adapter.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.KisAccessTokenStorePort;
import seokhoon.trade.domain.kis.*;

import java.time.Instant;
import java.util.Optional;

@Component
@ConditionalOnProperty(name="tradeguard.kis.token-cache-mode",
        havingValue="DB")
public class KisAccessTokenPersistenceAdapter
        implements KisAccessTokenStorePort {
    private final KisAccessTokenJpaRepository repository;

    public KisAccessTokenPersistenceAdapter(
            KisAccessTokenJpaRepository repository
    ) {
        this.repository=repository;
    }

    @Override
    @Transactional(readOnly=true)
    public Optional<StoredKisAccessToken> findByEnvironment(
            KisEnvironment environment) {
        return repository.findByEnvironment(environment)
                .map(KisAccessTokenEntity::toDomain);
    }

    @Override
    @Transactional
    public StoredKisAccessToken save(StoredKisAccessToken token) {
        KisAccessTokenEntity entity=repository.findByEnvironment(
                token.environment()).orElseThrow();
        entity.updateToken(token,Instant.now());
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    @Transactional
    public boolean tryAcquireRefreshLock(KisEnvironment environment,
            String owner,Instant now,Instant staleBefore) {
        return repository.tryAcquireRefreshLock(environment,owner,now,
                staleBefore) == 1;
    }

    @Override
    @Transactional
    public void releaseRefreshLock(KisEnvironment environment,String owner,
            Instant now) {
        repository.releaseRefreshLock(environment,owner,now);
    }

    @Override
    @Transactional
    public void clear(KisEnvironment environment,Instant now) {
        repository.clear(environment,now);
    }
}
