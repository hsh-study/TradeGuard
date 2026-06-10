package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.EarlyMarketPerformancePort;
import seokhoon.trade.domain.market.EarlyMarketCandidatePerformance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class EarlyMarketPerformancePersistenceAdapter
        implements EarlyMarketPerformancePort {
    private final EarlyMarketCandidatePerformanceJpaRepository repository;

    public EarlyMarketPerformancePersistenceAdapter(
            EarlyMarketCandidatePerformanceJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public EarlyMarketCandidatePerformance save(
            EarlyMarketCandidatePerformance performance
    ) {
        EarlyMarketCandidatePerformanceEntity entity = repository
                .findBySignalId(performance.signalId())
                .orElseGet(() -> EarlyMarketCandidatePerformanceEntity.from(performance));
        entity.update(performance);
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyMarketCandidatePerformance> findByTradeDate(LocalDate tradeDate) {
        return repository.findByTradeDateOrderBySignalIdAsc(tradeDate)
                .stream()
                .map(EarlyMarketCandidatePerformanceEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EarlyMarketCandidatePerformance> findBySignalId(long signalId) {
        return repository.findBySignalId(signalId)
                .map(EarlyMarketCandidatePerformanceEntity::toDomain);
    }
}
