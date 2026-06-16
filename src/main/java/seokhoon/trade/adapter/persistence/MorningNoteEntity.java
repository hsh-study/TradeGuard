package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.MorningNote;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "morning_notes", uniqueConstraints = @UniqueConstraint(
        name = "uk_morning_note_trade_date", columnNames = "trade_date"))
public class MorningNoteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "market_summary", nullable = false, columnDefinition = "TEXT")
    private String marketSummary;
    @Column(name = "sector_summary", nullable = false, columnDefinition = "TEXT")
    private String sectorSummary;
    @Column(name = "portfolio_impact_summary", nullable = false, columnDefinition = "TEXT")
    private String portfolioImpactSummary;
    @Column(name = "watchlist_summary", nullable = false, columnDefinition = "TEXT")
    private String watchlistSummary;
    @Column(name = "action_items", nullable = false, columnDefinition = "TEXT")
    private String actionItems;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MorningNoteEntity() {
    }

    static MorningNoteEntity from(MorningNote value) {
        MorningNoteEntity entity = new MorningNoteEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(MorningNote value) {
        tradeDate = value.tradeDate();
        marketSummary = value.marketSummary();
        sectorSummary = value.sectorSummary();
        portfolioImpactSummary = value.portfolioImpactSummary();
        watchlistSummary = value.watchlistSummary();
        actionItems = value.actionItems();
        createdAt = value.createdAt();
    }

    MorningNote toDomain() {
        return new MorningNote(id, tradeDate, marketSummary, sectorSummary,
                portfolioImpactSummary, watchlistSummary, actionItems, createdAt);
    }
}
