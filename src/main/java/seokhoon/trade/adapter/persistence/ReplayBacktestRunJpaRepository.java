package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReplayBacktestRunJpaRepository extends JpaRepository<ReplayBacktestRunEntity, Long> {
    Optional<ReplayBacktestRunEntity> findFirstByOrderByCreatedAtDescIdDesc();
}
