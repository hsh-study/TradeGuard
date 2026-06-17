package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.EarningsEvent;
import seokhoon.trade.domain.research.EarningsEventStatus;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "earnings_events", uniqueConstraints = @UniqueConstraint(
        name = "uk_earnings_event_stock_quarter",
        columnNames = {"stock_code", "fiscal_year", "fiscal_quarter"}))
public class EarningsEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;
    @Column(name = "fiscal_quarter", nullable = false)
    private int fiscalQuarter;
    @Column(name = "expected_announcement_date", nullable = false)
    private LocalDate expectedAnnouncementDate;
    @Column(name = "actual_announcement_date")
    private LocalDate actualAnnouncementDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EarningsEventStatus status;
    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EarningsEventEntity() {
    }

    static EarningsEventEntity from(EarningsEvent value) {
        EarningsEventEntity entity = new EarningsEventEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(EarningsEvent value) {
        stockCode = value.stockCode();
        fiscalYear = value.fiscalYear();
        fiscalQuarter = value.fiscalQuarter();
        expectedAnnouncementDate = value.expectedAnnouncementDate();
        actualAnnouncementDate = value.actualAnnouncementDate();
        status = value.status();
        memo = value.memo();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    EarningsEvent toDomain() {
        return new EarningsEvent(id, stockCode, fiscalYear, fiscalQuarter,
                expectedAnnouncementDate, actualAnnouncementDate, status, memo,
                createdAt, updatedAt);
    }
}
