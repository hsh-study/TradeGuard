package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.market.MarketIndex;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "market_indices", uniqueConstraints = @UniqueConstraint(
        name = "uk_market_index_code_date", columnNames = {"index_code", "trade_date"}))
public class MarketIndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "index_code", nullable = false, length = 30)
    private String indexCode;
    @Column(name = "index_name", nullable = false, length = 100)
    private String indexName;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "close_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal closePrice;
    @Column(name = "change_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal changeRate;
    @Column(name = "trading_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal tradingValue;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MarketIndexEntity() {
    }

    static MarketIndexEntity from(MarketIndex value) {
        MarketIndexEntity entity = new MarketIndexEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(MarketIndex value) {
        indexCode = value.indexCode();
        indexName = value.indexName();
        tradeDate = value.tradeDate();
        closePrice = value.closePrice();
        changeRate = value.changeRate();
        tradingValue = value.tradingValue();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    MarketIndex toDomain() {
        return new MarketIndex(id, indexCode, indexName, tradeDate, closePrice,
                changeRate, tradingValue, createdAt, updatedAt);
    }
}
