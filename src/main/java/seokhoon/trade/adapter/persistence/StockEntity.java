package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;

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

    public static StockEntity from(Stock stock) {
        return new StockEntity(stock.stockCode(), stock.stockName(), stock.market(), stock.active());
    }

    public Stock toDomain() {
        return new Stock(stockCode, stockName, market, active);
    }
}
