package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.PostEarningsReview;
import seokhoon.trade.domain.research.ThesisImpact;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "post_earnings_reviews", uniqueConstraints = @UniqueConstraint(
        name = "uk_post_earnings_review_event", columnNames = "earnings_event_id"))
public class PostEarningsReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "earnings_event_id", nullable = false)
    private long earningsEventId;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "review_date", nullable = false)
    private LocalDate reviewDate;
    @Column(name = "actual_revenue", nullable = false, precision = 19, scale = 4)
    private BigDecimal actualRevenue;
    @Column(name = "actual_operating_income", nullable = false, precision = 19, scale = 4)
    private BigDecimal actualOperatingIncome;
    @Column(name = "actual_net_income", nullable = false, precision = 19, scale = 4)
    private BigDecimal actualNetIncome;
    @Column(name = "actual_operating_margin", precision = 19, scale = 4)
    private BigDecimal actualOperatingMargin;
    @Column(name = "revenue_surprise_rate", precision = 19, scale = 4)
    private BigDecimal revenueSurpriseRate;
    @Column(name = "operating_income_surprise_rate", precision = 19, scale = 4)
    private BigDecimal operatingIncomeSurpriseRate;
    @Enumerated(EnumType.STRING)
    @Column(name = "thesis_impact", nullable = false, length = 30)
    private ThesisImpact thesisImpact;
    @Column(name = "review_summary", nullable = false, columnDefinition = "TEXT")
    private String reviewSummary;
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "action_items", nullable = false, columnDefinition = "TEXT")
    private List<String> actionItems;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PostEarningsReviewEntity() {
    }

    static PostEarningsReviewEntity from(PostEarningsReview value) {
        PostEarningsReviewEntity entity = new PostEarningsReviewEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(PostEarningsReview value) {
        earningsEventId = value.earningsEventId();
        stockCode = value.stockCode();
        reviewDate = value.reviewDate();
        actualRevenue = value.actualRevenue();
        actualOperatingIncome = value.actualOperatingIncome();
        actualNetIncome = value.actualNetIncome();
        actualOperatingMargin = value.actualOperatingMargin();
        revenueSurpriseRate = value.revenueSurpriseRate();
        operatingIncomeSurpriseRate = value.operatingIncomeSurpriseRate();
        thesisImpact = value.thesisImpact();
        reviewSummary = value.reviewSummary();
        actionItems = value.actionItems();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    PostEarningsReview toDomain() {
        return new PostEarningsReview(id, earningsEventId, stockCode, reviewDate,
                actualRevenue, actualOperatingIncome, actualNetIncome, actualOperatingMargin,
                revenueSurpriseRate, operatingIncomeSurpriseRate, thesisImpact, reviewSummary,
                actionItems, createdAt, updatedAt);
    }
}
