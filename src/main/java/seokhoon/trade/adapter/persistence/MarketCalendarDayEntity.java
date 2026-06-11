package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "market_calendar_days",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_market_calendar_market_trade_date",
                columnNames = {"market", "trade_date"}
        )
)
public class MarketCalendarDayEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 30)
    private String market;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "trading_day", nullable = false)
    private boolean tradingDay;
    @Column(name = "holiday_name", length = 200)
    private String holidayName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MarketCalendarSource source;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MarketCalendarDayEntity() {
    }

    static MarketCalendarDayEntity create(MarketCalendarDay day, Instant now) {
        MarketCalendarDayEntity entity = new MarketCalendarDayEntity();
        entity.market = day.market();
        entity.tradeDate = day.date();
        entity.createdAt = now;
        entity.update(day, now);
        return entity;
    }

    void update(MarketCalendarDay day, Instant now) {
        market = day.market();
        tradeDate = day.date();
        tradingDay = day.tradingDay();
        holidayName = day.holidayName();
        source = day.source();
        updatedAt = now;
    }

    MarketCalendarDay toDomain() {
        return new MarketCalendarDay(
                market,
                tradeDate,
                tradingDay,
                holidayName,
                source
        );
    }
}
