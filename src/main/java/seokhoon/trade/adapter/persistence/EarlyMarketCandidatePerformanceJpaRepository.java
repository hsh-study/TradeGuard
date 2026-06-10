package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EarlyMarketCandidatePerformanceJpaRepository
        extends JpaRepository<EarlyMarketCandidatePerformanceEntity, Long> {
    Optional<EarlyMarketCandidatePerformanceEntity> findBySignalId(Long signalId);

    List<EarlyMarketCandidatePerformanceEntity> findByTradeDateOrderBySignalIdAsc(
            LocalDate tradeDate
    );
}
