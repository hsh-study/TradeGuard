package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import seokhoon.trade.domain.market.BarInterval;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface EarlyMarketIntradayBarSnapshotJpaRepository
        extends JpaRepository<EarlyMarketIntradayBarSnapshotEntity, Long> {
    Optional<EarlyMarketIntradayBarSnapshotEntity>
    findByTradeDateAndStockCodeAndBarTimeAndIntervalType(
            LocalDate tradeDate,
            String stockCode,
            LocalTime barTime,
            BarInterval intervalType
    );

    List<EarlyMarketIntradayBarSnapshotEntity>
    findByTradeDateAndStockCodeOrderByBarTimeAsc(
            LocalDate tradeDate,
            String stockCode
    );
}
