package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IndicatorSnapshotJpaRepository extends JpaRepository<IndicatorSnapshotEntity, Long> {
    Optional<IndicatorSnapshotEntity> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

    List<IndicatorSnapshotEntity> findByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(
            String stockCode,
            LocalDate from,
            LocalDate to
    );
}
