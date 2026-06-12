package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MarketCalendarDayAuditJpaRepository
        extends JpaRepository<MarketCalendarDayAuditEntity, Long> {
    List<MarketCalendarDayAuditEntity>
    findByTradeDateBetweenOrderByCreatedAtDescIdDesc(
            LocalDate from,
            LocalDate to
    );
}
