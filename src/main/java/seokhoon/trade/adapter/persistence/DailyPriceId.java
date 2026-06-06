package seokhoon.trade.adapter.persistence;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class DailyPriceId implements Serializable {
    private String stockCode;
    private LocalDate tradeDate;

    protected DailyPriceId() {
    }

    public DailyPriceId(String stockCode, LocalDate tradeDate) {
        this.stockCode = stockCode;
        this.tradeDate = tradeDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyPriceId that)) return false;
        return Objects.equals(stockCode, that.stockCode) && Objects.equals(tradeDate, that.tradeDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockCode, tradeDate);
    }
}
