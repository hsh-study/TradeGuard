package seokhoon.trade.application.port.out;
import seokhoon.trade.domain.research.*; import java.time.Instant; import java.util.*;
public interface NewsRepositoryPort {
    Optional<NewsArticle> findBySourceHash(String sourceHash);
    Optional<NewsArticle> findByNormalizedTitleHash(String normalizedTitleHash);
    NewsArticle saveArticle(NewsArticle value);
    void saveMention(NewsStockMention value);
    NewsImportHistory saveHistory(NewsImportHistory value);
    List<NewsArticle> findArticles(String stockCode, Instant from, Instant to);
    List<NewsImportHistory> findHistories(String query, int limit);
    long countCollectedBetween(Instant from, Instant to);
    long countByImportanceBetween(NewsImportance importance, Instant from, Instant to);
    long countRiskBetween(Instant from, Instant to);
}
