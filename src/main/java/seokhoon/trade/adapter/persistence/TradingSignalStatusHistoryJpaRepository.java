package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradingSignalStatusHistoryJpaRepository
        extends JpaRepository<TradingSignalStatusHistoryEntity, Long> {
    List<TradingSignalStatusHistoryEntity>
    findByTradingSignalIdOrderByCreatedAtAscIdAsc(long tradingSignalId);
}
