package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyPriceJpaRepository extends JpaRepository<DailyPriceEntity, DailyPriceId> {
    List<DailyPriceEntity> findByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(
            String stockCode,
            LocalDate from,
            LocalDate to
    );
}
