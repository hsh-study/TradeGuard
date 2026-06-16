package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.InvestmentThesis;
import seokhoon.trade.domain.research.ThesisStatus;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "investment_theses")
public class InvestmentThesisEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(nullable = false)
    private String title;
    @Column(name = "core_assumption", nullable = false, columnDefinition = "TEXT")
    private String coreAssumption;
    @Column(name = "invalidation_condition", nullable = false, columnDefinition = "TEXT")
    private String invalidationCondition;
    @Column(name = "target_price")
    private BigDecimal targetPrice;
    @Column(name = "stop_loss_condition", nullable = false, columnDefinition = "TEXT")
    private String stopLossCondition;
    @Column(nullable = false)
    private int confidence;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThesisStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InvestmentThesisEntity() {
    }

    static InvestmentThesisEntity from(InvestmentThesis value) {
        InvestmentThesisEntity entity = new InvestmentThesisEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(InvestmentThesis value) {
        stockCode = value.stockCode();
        title = value.title();
        coreAssumption = value.coreAssumption();
        invalidationCondition = value.invalidationCondition();
        targetPrice = value.targetPrice();
        stopLossCondition = value.stopLossCondition();
        confidence = value.confidence();
        status = value.status();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    InvestmentThesis toDomain() {
        return new InvestmentThesis(id, stockCode, title, coreAssumption,
                invalidationCondition, targetPrice, stopLossCondition, confidence,
                status, createdAt, updatedAt);
    }
}
