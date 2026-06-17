package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.DartCorpMapping;
import seokhoon.trade.domain.stock.Market;

import java.time.Instant;

@Entity
@Table(name = "dart_corp_mappings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dart_corp_mapping_stock", columnNames = "stock_code"),
        @UniqueConstraint(name = "uk_dart_corp_mapping_corp", columnNames = "corp_code")
})
public class DartCorpMappingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "corp_code", nullable = false, length = 20)
    private String corpCode;
    @Column(name = "corp_name", nullable = false)
    private String corpName;
    @Enumerated(EnumType.STRING)
    @Column(name = "market", nullable = false, length = 30)
    private Market market;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DartCorpMappingEntity() {
    }

    static DartCorpMappingEntity from(DartCorpMapping value) {
        DartCorpMappingEntity entity = new DartCorpMappingEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(DartCorpMapping value) {
        stockCode = value.stockCode();
        corpCode = value.corpCode();
        corpName = value.corpName();
        market = value.market();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    DartCorpMapping toDomain() {
        return new DartCorpMapping(id, stockCode, corpCode, corpName, market, createdAt, updatedAt);
    }
}
