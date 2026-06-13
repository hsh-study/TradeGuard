package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import seokhoon.trade.domain.market.EarlyMarketRankingSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "early_market_ranking_snapshots")
public class EarlyMarketRankingSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
    @Column(name = "rank_no", nullable = false)
    private int rank;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;
    @Column(name = "current_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;
    @Column(name = "change_rate", nullable = false, precision = 19, scale = 4)
    private BigDecimal changeRate;
    @Column(nullable = false)
    private long volume;
    @Column(name = "trading_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal tradingValue;
    @Column(nullable = false, length = 100)
    private String source;

    protected EarlyMarketRankingSnapshotEntity() {
    }

    static EarlyMarketRankingSnapshotEntity from(
            EarlyMarketRankingSnapshot snapshot
    ) {
        EarlyMarketRankingSnapshotEntity entity =
                new EarlyMarketRankingSnapshotEntity();
        entity.tradeDate = snapshot.tradeDate();
        entity.capturedAt = snapshot.capturedAt();
        entity.rank = snapshot.rank();
        entity.stockCode = snapshot.stockCode();
        entity.stockName = snapshot.stockName();
        entity.currentPrice = snapshot.currentPrice();
        entity.changeRate = snapshot.changeRate();
        entity.volume = snapshot.volume();
        entity.tradingValue = snapshot.tradingValue();
        entity.source = snapshot.source();
        return entity;
    }

    EarlyMarketRankingSnapshot toDomain() {
        return new EarlyMarketRankingSnapshot(
                id,
                tradeDate,
                capturedAt,
                rank,
                stockCode,
                stockName,
                currentPrice,
                changeRate,
                volume,
                tradingValue,
                source
        );
    }
}
