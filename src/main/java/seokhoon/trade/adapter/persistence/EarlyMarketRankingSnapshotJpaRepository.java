package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EarlyMarketRankingSnapshotJpaRepository
        extends JpaRepository<EarlyMarketRankingSnapshotEntity, Long> {
    List<EarlyMarketRankingSnapshotEntity>
    findByTradeDateOrderByCapturedAtAscSourceAscRankAsc(LocalDate tradeDate);
}
