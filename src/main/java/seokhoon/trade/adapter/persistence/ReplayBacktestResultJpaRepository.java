package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReplayBacktestResultJpaRepository extends JpaRepository<ReplayBacktestResultEntity, Long> {
    List<ReplayBacktestResultEntity> findByRunIdOrderByTradeDateAscCandidateRankAsc(long runId);
}
