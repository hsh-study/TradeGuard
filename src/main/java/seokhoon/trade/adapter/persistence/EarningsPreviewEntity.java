package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.EarningsPreview;
import seokhoon.trade.domain.research.EarningsPreviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "earnings_previews")
public class EarningsPreviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "earnings_event_id", nullable = false)
    private long earningsEventId;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "preview_date", nullable = false)
    private LocalDate previewDate;
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "key_checkpoints", nullable = false, columnDefinition = "TEXT")
    private List<String> keyCheckpoints;
    @Column(name = "expected_revenue", precision = 19, scale = 4)
    private BigDecimal expectedRevenue;
    @Column(name = "expected_operating_income", precision = 19, scale = 4)
    private BigDecimal expectedOperatingIncome;
    @Column(name = "expected_net_income", precision = 19, scale = 4)
    private BigDecimal expectedNetIncome;
    @Column(name = "expected_operating_margin", precision = 19, scale = 4)
    private BigDecimal expectedOperatingMargin;
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "expected_risks", nullable = false, columnDefinition = "TEXT")
    private List<String> expectedRisks;
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "thesis_watch_points", nullable = false, columnDefinition = "TEXT")
    private List<String> thesisWatchPoints;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EarningsPreviewStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EarningsPreviewEntity() {
    }

    static EarningsPreviewEntity from(EarningsPreview value) {
        EarningsPreviewEntity entity = new EarningsPreviewEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(EarningsPreview value) {
        earningsEventId = value.earningsEventId();
        stockCode = value.stockCode();
        previewDate = value.previewDate();
        keyCheckpoints = value.keyCheckpoints();
        expectedRevenue = value.expectedRevenue();
        expectedOperatingIncome = value.expectedOperatingIncome();
        expectedNetIncome = value.expectedNetIncome();
        expectedOperatingMargin = value.expectedOperatingMargin();
        expectedRisks = value.expectedRisks();
        thesisWatchPoints = value.thesisWatchPoints();
        status = value.status();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    EarningsPreview toDomain() {
        return new EarningsPreview(id, earningsEventId, stockCode, previewDate,
                keyCheckpoints, expectedRevenue, expectedOperatingIncome, expectedNetIncome,
                expectedOperatingMargin, expectedRisks, thesisWatchPoints, status,
                createdAt, updatedAt);
    }
}
