package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketIndexJpaRepository extends JpaRepository<MarketIndexEntity, Long> {
    Optional<MarketIndexEntity> findByIndexCodeAndTradeDate(String indexCode, LocalDate tradeDate);
    List<MarketIndexEntity> findByTradeDateOrderByIndexCodeAsc(LocalDate tradeDate);
}
