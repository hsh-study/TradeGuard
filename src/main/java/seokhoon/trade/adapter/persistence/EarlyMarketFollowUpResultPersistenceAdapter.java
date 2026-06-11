package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.EarlyMarketFollowUpResultPort;
import seokhoon.trade.domain.market.EarlyMarketFollowUpRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class EarlyMarketFollowUpResultPersistenceAdapter
        implements EarlyMarketFollowUpResultPort {
    private final EarlyMarketFollowUpResultJpaRepository repository;

    public EarlyMarketFollowUpResultPersistenceAdapter(
            EarlyMarketFollowUpResultJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public EarlyMarketFollowUpRecord save(EarlyMarketFollowUpRecord result) {
        EarlyMarketFollowUpResultEntity entity = repository
                .findBySignalId(result.signalId())
                .orElseGet(() -> EarlyMarketFollowUpResultEntity.from(result));
        entity.update(result);
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyMarketFollowUpRecord> findByTradeDate(LocalDate tradeDate) {
        return repository.findByTradeDateOrderBySignalIdAsc(tradeDate)
                .stream()
                .map(EarlyMarketFollowUpResultEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EarlyMarketFollowUpRecord> findBySignalId(long signalId) {
        return repository.findBySignalId(signalId)
                .map(EarlyMarketFollowUpResultEntity::toDomain);
    }
}
