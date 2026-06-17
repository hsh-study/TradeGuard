package seokhoon.trade.domain.research;

import seokhoon.trade.domain.stock.Market;

import java.time.Instant;
import java.util.Objects;

public record DartCorpMapping(
        Long id,
        String stockCode,
        String corpCode,
        String corpName,
        Market market,
        Instant createdAt,
        Instant updatedAt
) {
    public DartCorpMapping {
        Objects.requireNonNull(stockCode, "stockCode");
        Objects.requireNonNull(corpCode, "corpCode");
        Objects.requireNonNull(corpName, "corpName");
        Objects.requireNonNull(market, "market");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
