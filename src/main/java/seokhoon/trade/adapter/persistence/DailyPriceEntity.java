package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@IdClass(DailyPriceId.class)
@Table(name = "daily_prices")
public class DailyPriceEntity {
    @Id
    private String stockCode;
    @Id
    private LocalDate tradeDate;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private long volume;
    private BigDecimal tradingValue;

    protected DailyPriceEntity() {
    }

    public DailyPriceEntity(String stockCode, LocalDate tradeDate, BigDecimal openPrice, BigDecimal highPrice,
                            BigDecimal lowPrice, BigDecimal closePrice, long volume, BigDecimal tradingValue) {
        this.stockCode = stockCode;
        this.tradeDate = tradeDate;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.tradingValue = tradingValue;
    }
}
