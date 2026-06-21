package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.NewsRepositoryPort;
import seokhoon.trade.domain.research.*;
import java.math.BigDecimal; import java.time.Instant; import java.util.*;

@Component
@Transactional
public class NewsPersistenceAdapter implements NewsRepositoryPort {
    private final NewsArticleJpaRepository articles; private final NewsMentionJpaRepository mentions;
    private final NewsImportHistoryJpaRepository histories;
    public NewsPersistenceAdapter(NewsArticleJpaRepository a, NewsMentionJpaRepository m, NewsImportHistoryJpaRepository h){articles=a;mentions=m;histories=h;}
    @Override public Optional<NewsArticle> findBySourceHash(String hash){return articles.findBySourceHash(hash).map(NewsArticleEntity::domain);}
    @Override public Optional<NewsArticle> findByNormalizedTitleHash(String hash){return articles.findFirstByNormalizedTitleHashOrderByCollectedAtDesc(hash).map(NewsArticleEntity::domain);}
    @Override public NewsArticle saveArticle(NewsArticle v){try{return articles.save(NewsArticleEntity.from(v)).domain();}catch(DataIntegrityViolationException e){return findBySourceHash(v.sourceHash()).orElseThrow(()->e);}}
    @Override public void saveMention(NewsStockMention v){if(!mentions.existsByNewsArticleIdAndStockCode(v.newsArticleId(),v.stockCode()))mentions.save(NewsMentionEntity.from(v));}
    @Override public NewsImportHistory saveHistory(NewsImportHistory v){return histories.save(NewsImportHistoryEntity.from(v)).domain();}
    @Override @Transactional(readOnly=true) public List<NewsArticle> findArticles(String stockCode,Instant from,Instant to){return articles.findNews(stockCode,from,to).stream().map(NewsArticleEntity::domain).toList();}
    @Override @Transactional(readOnly=true) public List<NewsImportHistory> findHistories(String query,int limit){return histories.findRecent(query,PageRequest.of(0,limit)).stream().map(NewsImportHistoryEntity::domain).toList();}
    @Override public long countCollectedBetween(Instant from,Instant to){return articles.countByCollectedAtBetween(from,to);}
    @Override public long countByImportanceBetween(NewsImportance i,Instant from,Instant to){return articles.countByImportanceAndCollectedAtBetween(i,from,to);}
    @Override public long countRiskBetween(Instant from,Instant to){return articles.countRisk(from,to,Set.of(NewsCategory.RISK,NewsCategory.REGULATORY),NewsSentiment.NEGATIVE);}
}

@Entity @Table(name="news_articles") class NewsArticleEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; String provider;
    @Column(length=500) String title; @Column(length=1500) String summary;
    @Column(name="origin_link",length=1000) String originLink; @Column(length=1000) String link;
    String publisher; @Column(name="published_at") Instant publishedAt; @Column(name="collected_at") Instant collectedAt;
    @Column(name="query_text",length=100) String query; @Column(name="normalized_title_hash",length=64) String normalizedTitleHash;
    @Column(name="source_hash",length=64,unique=true) String sourceHash; @Enumerated(EnumType.STRING) NewsCategory category;
    @Enumerated(EnumType.STRING) NewsSentiment sentiment; @Enumerated(EnumType.STRING) NewsImportance importance;
    @Column(name="short_reason",length=500) String shortReason; @Column(name="created_at") Instant createdAt; @Column(name="updated_at") Instant updatedAt;
    static NewsArticleEntity from(NewsArticle v){var e=new NewsArticleEntity();e.id=v.id();e.provider=v.provider();e.title=v.title();e.summary=v.summary();e.originLink=v.originLink();e.link=v.link();e.publisher=v.publisher();e.publishedAt=v.publishedAt();e.collectedAt=v.collectedAt();e.query=v.query();e.normalizedTitleHash=v.normalizedTitleHash();e.sourceHash=v.sourceHash();e.category=v.category();e.sentiment=v.sentiment();e.importance=v.importance();e.shortReason=v.shortReason();e.createdAt=v.createdAt();e.updatedAt=v.updatedAt();return e;}
    NewsArticle domain(){return new NewsArticle(id,provider,title,summary,originLink,link,publisher,publishedAt,collectedAt,query,normalizedTitleHash,sourceHash,category,sentiment,importance,shortReason,createdAt,updatedAt);}
}
@Entity @Table(name="news_stock_mentions") class NewsMentionEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(name="news_article_id") Long newsArticleId;
    @Column(name="stock_code") String stockCode; @Column(name="stock_name") String stockName;
    @Enumerated(EnumType.STRING) @Column(name="match_type") NewsMatchType matchType; BigDecimal confidence; @Column(name="created_at") Instant createdAt;
    static NewsMentionEntity from(NewsStockMention v){var e=new NewsMentionEntity();e.id=v.id();e.newsArticleId=v.newsArticleId();e.stockCode=v.stockCode();e.stockName=v.stockName();e.matchType=v.matchType();e.confidence=v.confidence();e.createdAt=v.createdAt();return e;}
}
@Entity @Table(name="news_import_histories") class NewsImportHistoryEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(name="query_text") String query;
    @Column(name="requested_display") int requestedDisplay; @Column(name="fetched_count") int fetchedCount;
    @Column(name="saved_count") int savedCount; @Column(name="duplicated_count") int duplicatedCount;
    @Enumerated(EnumType.STRING) NewsImportStatus status; @Column(name="failure_reason") String failureReason;
    @Column(name="started_at") Instant startedAt; @Column(name="finished_at") Instant finishedAt;
    static NewsImportHistoryEntity from(NewsImportHistory v){var e=new NewsImportHistoryEntity();e.id=v.id();e.query=v.query();e.requestedDisplay=v.requestedDisplay();e.fetchedCount=v.fetchedCount();e.savedCount=v.savedCount();e.duplicatedCount=v.duplicatedCount();e.status=v.status();e.failureReason=v.failureReason();e.startedAt=v.startedAt();e.finishedAt=v.finishedAt();return e;}
    NewsImportHistory domain(){return new NewsImportHistory(id,query,requestedDisplay,fetchedCount,savedCount,duplicatedCount,status,failureReason,startedAt,finishedAt);}
}
interface NewsArticleJpaRepository extends JpaRepository<NewsArticleEntity,Long>{
    Optional<NewsArticleEntity> findBySourceHash(String hash); Optional<NewsArticleEntity> findFirstByNormalizedTitleHashOrderByCollectedAtDesc(String hash);
    @Query("select distinct a from NewsArticleEntity a left join NewsMentionEntity m on m.newsArticleId=a.id where (:code is null or m.stockCode=:code) and a.collectedAt between :from and :to order by a.publishedAt desc, a.collectedAt desc") List<NewsArticleEntity> findNews(@Param("code")String code,@Param("from")Instant from,@Param("to")Instant to);
    long countByCollectedAtBetween(Instant from,Instant to); long countByImportanceAndCollectedAtBetween(NewsImportance i,Instant from,Instant to);
    @Query("select count(a) from NewsArticleEntity a where a.collectedAt between :from and :to and (a.category in :categories or a.sentiment=:sentiment)")
    long countRisk(@Param("from")Instant from,@Param("to")Instant to,
            @Param("categories")Set<NewsCategory> categories,@Param("sentiment")NewsSentiment sentiment);
}
interface NewsMentionJpaRepository extends JpaRepository<NewsMentionEntity,Long>{boolean existsByNewsArticleIdAndStockCode(Long id,String code);}
interface NewsImportHistoryJpaRepository extends JpaRepository<NewsImportHistoryEntity,Long>{
    @Query("select h from NewsImportHistoryEntity h where (:query is null or h.query=:query) order by h.startedAt desc") List<NewsImportHistoryEntity> findRecent(@Param("query")String query,org.springframework.data.domain.Pageable pageable);
}
