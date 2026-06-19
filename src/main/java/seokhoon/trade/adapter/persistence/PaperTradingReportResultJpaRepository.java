package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaperTradingReportResultJpaRepository extends JpaRepository<PaperTradingReportResultEntity, Long> {
    List<PaperTradingReportResultEntity> findByRunIdOrderByStrategyAscCandidateRankAsc(long runId);
}
