package seokhoon.trade.adapter.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.EarlyMarketStrategyExperimentPort;
import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.util.List;
import java.util.Optional;

@Component
public class EarlyMarketStrategyExperimentPersistenceAdapter
        implements EarlyMarketStrategyExperimentPort {
    private final EarlyMarketStrategyExperimentJpaRepository repository;

    public EarlyMarketStrategyExperimentPersistenceAdapter(
            EarlyMarketStrategyExperimentJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public EarlyMarketStrategyExperiment save(
            EarlyMarketStrategyExperiment experiment
    ) {
        return repository.save(EarlyMarketStrategyExperimentEntity.from(experiment))
                .toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EarlyMarketStrategyExperiment> findById(long id) {
        return repository.findById(id)
                .map(EarlyMarketStrategyExperimentEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyMarketStrategyExperiment> findRecent(int limit) {
        return repository.findAllByOrderByCreatedAtDescIdDesc(
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(EarlyMarketStrategyExperimentEntity::toDomain)
                .toList();
    }
}
