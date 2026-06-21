package seokhoon.trade.domain.research;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record MorningNote(
        Long id,
        LocalDate tradeDate,
        String marketSummary,
        String sectorSummary,
        String portfolioImpactSummary,
        String watchlistSummary,
        String newsSummary,
        String actionItems,
        Instant createdAt
) {
    public MorningNote {
        Objects.requireNonNull(tradeDate, "tradeDate");
        Objects.requireNonNull(marketSummary, "marketSummary");
        Objects.requireNonNull(sectorSummary, "sectorSummary");
        Objects.requireNonNull(portfolioImpactSummary, "portfolioImpactSummary");
        Objects.requireNonNull(watchlistSummary, "watchlistSummary");
        Objects.requireNonNull(newsSummary, "newsSummary");
        Objects.requireNonNull(actionItems, "actionItems");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public MorningNote(Long id, LocalDate tradeDate, String marketSummary, String sectorSummary,
            String portfolioImpactSummary, String watchlistSummary, String actionItems, Instant createdAt) {
        this(id, tradeDate, marketSummary, sectorSummary, portfolioImpactSummary, watchlistSummary,
                "", actionItems, createdAt);
    }
}
