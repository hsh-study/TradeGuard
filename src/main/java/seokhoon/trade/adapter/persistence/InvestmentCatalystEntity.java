package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "investment_catalysts")
public class InvestmentCatalystEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", length = 20)
    private String stockCode;
    @Column(nullable = false)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(name = "catalyst_type", nullable = false)
    private CatalystType catalystType;
    @Column(name = "expected_date", nullable = false)
    private LocalDate expectedDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CatalystImportance importance;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CatalystStatus status;
    @Column(name = "source_url", length = 1000)
    private String sourceUrl;
    @Column(columnDefinition = "TEXT")
    private String memo;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InvestmentCatalystEntity() {
    }

    static InvestmentCatalystEntity from(InvestmentCatalyst value) {
        InvestmentCatalystEntity entity = new InvestmentCatalystEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(InvestmentCatalyst value) {
        stockCode = value.stockCode();
        title = value.title();
        catalystType = value.catalystType();
        expectedDate = value.expectedDate();
        importance = value.importance();
        status = value.status();
        sourceUrl = value.sourceUrl();
        memo = value.memo();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    InvestmentCatalyst toDomain() {
        return new InvestmentCatalyst(id, stockCode, title, catalystType,
                expectedDate, importance, status, sourceUrl, memo, createdAt, updatedAt);
    }
}
