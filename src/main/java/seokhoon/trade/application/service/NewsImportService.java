package seokhoon.trade.application.service;

import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.in.NewsUseCase; import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.NaverNewsProperties; import seokhoon.trade.domain.research.*; import seokhoon.trade.domain.stock.Stock;
import java.math.BigDecimal; import java.nio.charset.StandardCharsets; import java.security.*; import java.time.*; import java.util.*;

@Service
public class NewsImportService implements NewsUseCase {
    private final NewsProviderPort provider; private final NewsRepositoryPort repository; private final StockPort stocks;
    private final NaverNewsProperties properties; private final NewsClassificationService classifier; private final OperationalMetricsPort metrics; private final Clock clock;
    @Autowired public NewsImportService(NewsProviderPort p,NewsRepositoryPort r,StockPort s,NaverNewsProperties props,NewsClassificationService c,OperationalMetricsPort m){this(p,r,s,props,c,m,Clock.systemUTC());}
    NewsImportService(NewsProviderPort p,NewsRepositoryPort r,StockPort s,NaverNewsProperties props,NewsClassificationService c,OperationalMetricsPort m,Clock clock){provider=p;repository=r;stocks=s;properties=props;classifier=c;metrics=m;this.clock=clock;}
    @Override @Transactional public ImportResult importQuery(String query,Integer display){return importInternal(query,display,null);}
    @Override @Transactional public ImportResult importStock(String stockCode){Stock stock=stocks.findByStockCode(stockCode).orElseThrow(()->new IllegalArgumentException("stock not found"));return importInternal(stock.stockName(),null,stock);}
    @Override public List<ImportResult> importWatchlist(){return stocks.findAll().stream().filter(Stock::active).limit(properties.getMaxSymbolsPerRun()).map(s->importInternal(s.stockName(),null,s)).toList();}
    private ImportResult importInternal(String raw,Integer requested,Stock stock){Instant started=clock.instant();String query=validateQuery(raw);int display=requested==null?properties.getMaxDisplay():Math.min(requested,properties.getMaxDisplay());
        if(!properties.isProviderEnabled())return finish(query,display,0,0,0,NewsImportStatus.SKIPPED,"NEWS_PROVIDER_DISABLED",started);
        try{properties.validateEnabled();List<NewsProviderPort.ProviderNews> fetched=provider.search(query,display);int saved=0,duplicates=0;Instant cutoff=clock.instant().minus(Duration.ofHours(properties.getLookbackHours()));
            for(var item:fetched){if(item.publishedAt()!=null&&item.publishedAt().isBefore(cutoff))continue;String title=clip(item.title(),500),summary=clip(item.summary(),1500),sourceHash=hash(first(item.originLink(),item.link(),title)),titleHash=hash(normalize(title));
                Optional<NewsArticle> existing=repository.findBySourceHash(sourceHash).or(()->repository.findByNormalizedTitleHash(titleHash));NewsArticle article;
                if(existing.isPresent()){article=existing.orElseThrow();duplicates++;}else{var cls=classifier.classify(title,summary);Instant now=clock.instant();article=repository.saveArticle(new NewsArticle(null,"NAVER",title,summary,clip(item.originLink(),1000),clip(item.link(),1000),clip(item.publisher(),255),item.publishedAt(),now,query,titleHash,sourceHash,cls.category(),cls.sentiment(),cls.importance(),clip(cls.reason(),500),now,now));saved++;metrics.recordResearchNewsClassification(cls.category().name().toLowerCase(Locale.ROOT),cls.sentiment().name().toLowerCase(Locale.ROOT),"classified");}
                if(stock!=null)repository.saveMention(new NewsStockMention(null,article.id(),stock.stockCode(),stock.stockName(),NewsMatchType.EXACT_NAME,new BigDecimal("1.0000"),clock.instant()));}
            NewsImportStatus status=saved+duplicates==fetched.size()?NewsImportStatus.SUCCESS:NewsImportStatus.PARTIAL;return finish(query,display,fetched.size(),saved,duplicates,status,null,started);
        }catch(RuntimeException e){return finish(query,display,0,0,0,NewsImportStatus.FAILED,safeReason(e),started);}}
    @Override @Transactional(readOnly=true) public List<NewsArticle> find(String code,Instant from,Instant to){if(from==null||to==null||from.isAfter(to))throw new IllegalArgumentException("valid from/to required");return repository.findArticles(code,from,to);}
    @Override @Transactional(readOnly=true) public List<NewsImportHistory> histories(String query){return repository.findHistories(query,50);}
    private ImportResult finish(String q,int d,int f,int s,int dup,NewsImportStatus status,String reason,Instant started){repository.saveHistory(new NewsImportHistory(null,q,d,f,s,dup,status,reason,started,clock.instant()));metrics.recordResearchNewsImport("naver",status.name().toLowerCase(Locale.ROOT));return new ImportResult(q,f,s,dup,status,reason);}
    private String validateQuery(String q){if(q==null||q.isBlank())throw new IllegalArgumentException("query required");String v=q.trim();if(v.length()>properties.getQueryMaxLength())throw new IllegalArgumentException("query too long");return v;}
    private static String safeReason(Throwable e){return e instanceof IllegalStateException&&e.getMessage()!=null&&e.getMessage().startsWith("Naver News credentials")?"NAVER_NEWS_CREDENTIALS_MISSING":"NAVER_NEWS_IMPORT_FAILED";}
    private static String normalize(String v){return v.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-z가-힣]","");}
    private static String hash(String v){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(b);}catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable");}}
    private static String first(String...v){return Arrays.stream(v).filter(x->x!=null&&!x.isBlank()).findFirst().orElse("unknown");}
    private static String clip(String v,int max){if(v==null)return null;return v.length()<=max?v:v.substring(0,max);}
}
