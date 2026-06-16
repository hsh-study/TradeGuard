package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.market.StockSectorMapping;

import java.time.Instant;

@Entity
@Table(name = "stock_sector_mappings", uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_sector_mapping", columnNames = {"stock_code", "sector_code"}))
public class StockSectorMappingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "sector_code", nullable = false, length = 50)
    private String sectorCode;
    @Column(name = "source", nullable = false, length = 100)
    private String source;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockSectorMappingEntity() {
    }

    static StockSectorMappingEntity from(StockSectorMapping value) {
        StockSectorMappingEntity entity = new StockSectorMappingEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(StockSectorMapping value) {
        stockCode = value.stockCode();
        sectorCode = value.sectorCode();
        source = value.source();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    StockSectorMapping toDomain() {
        return new StockSectorMapping(id, stockCode, sectorCode, source, createdAt, updatedAt);
    }
}
