package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.TradingAccountEncryptionPort;
import seokhoon.trade.application.port.out.TradingAccountPort;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.order.TradingAccount;

import java.util.List;
import java.util.Optional;

@Component
public class TradingAccountPersistenceAdapter implements TradingAccountPort {
    private final TradingAccountJpaRepository repository;
    private final TradingAccountEncryptionPort encryption;

    public TradingAccountPersistenceAdapter(TradingAccountJpaRepository repository,
            TradingAccountEncryptionPort encryption) {
        this.repository = repository; this.encryption = encryption;
    }

    @Override public TradingAccount save(TradingAccount account) {
        TradingAccountEntity entity = account.id() == null ? new TradingAccountEntity()
                : repository.findById(account.id()).orElseThrow();
        entity.update(account, encryption);
        return repository.saveAndFlush(entity).toDomain(encryption);
    }
    @Override public List<TradingAccount> findAll() {
        if (!encryption.configured()) return List.of();
        return repository.findAllByOrderByEnvironmentAscAliasAsc().stream().map(e -> e.toDomain(encryption)).toList();
    }
    @Override public Optional<TradingAccount> findById(long id) {
        if (!encryption.configured()) return Optional.empty();
        return repository.findById(id).map(e -> e.toDomain(encryption));
    }
    @Override public Optional<TradingAccount> findPrimary(KisEnvironment environment) {
        if (!encryption.configured()) return Optional.empty();
        return repository.findFirstByEnvironmentAndActiveTrueAndPrimaryAccountTrue(environment)
                .map(e -> e.toDomain(encryption));
    }
    @Override public Optional<TradingAccount> findPrimary() {
        if (!encryption.configured()) return Optional.empty();
        return repository.findFirstByActiveTrueAndPrimaryAccountTrue().map(e -> e.toDomain(encryption));
    }
    @Override public void clearPrimary(KisEnvironment environment) { repository.clearPrimary(environment); }
    @Override public void clearPrimary() { repository.clearAllPrimary(); }
}
