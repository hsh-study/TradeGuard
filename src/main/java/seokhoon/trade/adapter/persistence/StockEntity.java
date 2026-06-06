package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import seokhoon.trade.domain.stock.Market;

@Entity
@Table(name = "stocks")
public class StockEntity {
    @Id
    private String stockCode;
    private String stockName;
    @Enumerated(EnumType.STRING)
    private Market market;
    private boolean active;

    protected StockEntity() {
    }

    public StockEntity(String stockCode, String stockName, Market market, boolean active) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.market = market;
        this.active = active;
    }

    public String getStockCode() { return stockCode; }
    public String getStockName() { return stockName; }
    public Market getMarket() { return market; }
    public boolean isActive() { return active; }
}
