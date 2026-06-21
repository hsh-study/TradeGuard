package seokhoon.trade.application.service;
import org.springframework.stereotype.Service; import seokhoon.trade.application.port.out.NewsRepositoryPort; import seokhoon.trade.domain.research.*; import java.time.*; import java.util.List;
@Service public class NewsCatalystCandidateService {private final NewsRepositoryPort news;public NewsCatalystCandidateService(NewsRepositoryPort n){news=n;}public List<NewsArticle> highImportanceCandidates(String stockCode,Instant from,Instant to){return news.findArticles(stockCode,from,to).stream().filter(v->v.importance()==NewsImportance.HIGH).toList();}}
