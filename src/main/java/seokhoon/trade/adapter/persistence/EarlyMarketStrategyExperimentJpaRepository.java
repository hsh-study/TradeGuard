package seokhoon.trade.adapter.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EarlyMarketStrategyExperimentJpaRepository
        extends JpaRepository<EarlyMarketStrategyExperimentEntity, Long> {
    List<EarlyMarketStrategyExperimentEntity> findAllByOrderByCreatedAtDescIdDesc(
            Pageable pageable
    );
}
