package seokhoon.trade.domain.research;
import java.math.BigDecimal; import java.time.Instant;
public record NewsStockMention(Long id, Long newsArticleId, String stockCode,
        String stockName, NewsMatchType matchType, BigDecimal confidence, Instant createdAt) {}
