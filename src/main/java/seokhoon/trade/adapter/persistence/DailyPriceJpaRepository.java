package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPriceJpaRepository extends JpaRepository<DailyPriceEntity, DailyPriceId> {
    List<DailyPriceEntity> findByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(
            String stockCode,
            LocalDate from,
            LocalDate to
    );

    Optional<DailyPriceEntity> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

    Optional<DailyPriceEntity> findFirstByStockCodeOrderByTradeDateDesc(String stockCode);

    List<DailyPriceEntity> findByStockCodeAndTradeDateGreaterThanOrderByTradeDateAsc(
            String stockCode,
            LocalDate tradeDate
    );
}
