package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EarlyMarketAfterHoursSnapshotJpaRepository
        extends JpaRepository<EarlyMarketAfterHoursSnapshotEntity, Long> {
    Optional<EarlyMarketAfterHoursSnapshotEntity>
    findByTradeDateAndPreviousTradingDayAndStockCode(
            LocalDate tradeDate,
            LocalDate previousTradingDay,
            String stockCode
    );

    List<EarlyMarketAfterHoursSnapshotEntity>
    findByTradeDateOrderByStockCodeAsc(LocalDate tradeDate);
}
