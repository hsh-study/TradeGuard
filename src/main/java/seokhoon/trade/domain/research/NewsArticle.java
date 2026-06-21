package seokhoon.trade.domain.research;

import java.time.Instant;

public record NewsArticle(Long id, String provider, String title, String summary,
        String originLink, String link, String publisher, Instant publishedAt,
        Instant collectedAt, String query, String normalizedTitleHash, String sourceHash,
        NewsCategory category, NewsSentiment sentiment, NewsImportance importance,
        String shortReason, Instant createdAt, Instant updatedAt) {}
