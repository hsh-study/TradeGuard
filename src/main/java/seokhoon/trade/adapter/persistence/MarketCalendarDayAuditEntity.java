package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import seokhoon.trade.domain.market.MarketCalendarDayAudit;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "market_calendar_day_audits")
public class MarketCalendarDayAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 30)
    private String market;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "before_trading_day")
    private Boolean beforeTradingDay;
    @Column(name = "after_trading_day", nullable = false)
    private boolean afterTradingDay;
    @Column(name = "before_holiday_name", length = 200)
    private String beforeHolidayName;
    @Column(name = "after_holiday_name", length = 200)
    private String afterHolidayName;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(nullable = false, length = 100)
    private String actor;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MarketCalendarDayAuditEntity() {
    }

    static MarketCalendarDayAuditEntity from(MarketCalendarDayAudit audit) {
        MarketCalendarDayAuditEntity entity = new MarketCalendarDayAuditEntity();
        entity.market = audit.market();
        entity.tradeDate = audit.date();
        entity.beforeTradingDay = audit.beforeTradingDay();
        entity.afterTradingDay = audit.afterTradingDay();
        entity.beforeHolidayName = audit.beforeHolidayName();
        entity.afterHolidayName = audit.afterHolidayName();
        entity.reason = audit.reason();
        entity.actor = audit.actor();
        entity.createdAt = audit.createdAt();
        return entity;
    }

    MarketCalendarDayAudit toDomain() {
        return new MarketCalendarDayAudit(
                id,
                market,
                tradeDate,
                beforeTradingDay,
                afterTradingDay,
                beforeHolidayName,
                afterHolidayName,
                reason,
                actor,
                createdAt
        );
    }
}
