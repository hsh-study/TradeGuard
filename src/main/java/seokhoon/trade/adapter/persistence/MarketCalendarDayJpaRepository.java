package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketCalendarDayJpaRepository
        extends JpaRepository<MarketCalendarDayEntity, Long> {
    Optional<MarketCalendarDayEntity> findByMarketAndTradeDate(
            String market,
            LocalDate tradeDate
    );

    List<MarketCalendarDayEntity> findByMarketAndTradeDateBetweenOrderByTradeDateAsc(
            String market,
            LocalDate from,
            LocalDate to
    );

    boolean existsByMarketAndTradeDateBetween(
            String market,
            LocalDate from,
            LocalDate to
    );

    Optional<MarketCalendarDayEntity>
    findFirstByMarketAndTradingDayTrueAndTradeDateBeforeOrderByTradeDateDesc(
            String market,
            LocalDate date
    );

    Optional<MarketCalendarDayEntity>
    findFirstByMarketAndTradingDayTrueAndTradeDateAfterOrderByTradeDateAsc(
            String market,
            LocalDate date
    );
}
