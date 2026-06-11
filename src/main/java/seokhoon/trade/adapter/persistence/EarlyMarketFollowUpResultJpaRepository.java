package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EarlyMarketFollowUpResultJpaRepository
        extends JpaRepository<EarlyMarketFollowUpResultEntity, Long> {
    Optional<EarlyMarketFollowUpResultEntity> findBySignalId(Long signalId);

    List<EarlyMarketFollowUpResultEntity> findByTradeDateOrderBySignalIdAsc(
            LocalDate tradeDate
    );
}
