package seokhoon.trade.adapter.web;
import org.springframework.format.annotation.DateTimeFormat; import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.NewsUseCase; import seokhoon.trade.domain.research.*; import java.time.Instant; import java.util.List;
@RestController @RequestMapping("/api/research/news")
public class NewsController {
    private final NewsUseCase useCase; public NewsController(NewsUseCase u){useCase=u;}
    @PostMapping("/import") NewsUseCase.ImportResult importQuery(@RequestParam String query,@RequestParam(required=false)Integer display){return useCase.importQuery(query,display);}
    @PostMapping("/import-stock") NewsUseCase.ImportResult importStock(@RequestParam String stockCode){return useCase.importStock(stockCode);}
    @PostMapping("/import-watchlist") List<NewsUseCase.ImportResult> importWatchlist(){return useCase.importWatchlist();}
    @GetMapping List<NewsView> find(@RequestParam(required=false)String stockCode,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to){return useCase.find(stockCode,from,to).stream().map(NewsView::from).toList();}
    @GetMapping("/import-histories") List<NewsImportHistory> histories(@RequestParam(required=false)String query){return useCase.histories(query);}
    public record NewsView(Long id,String title,String summary,String link,String publisher,Instant publishedAt,Instant collectedAt,NewsCategory category,NewsSentiment sentiment,NewsImportance importance,String shortReason){static NewsView from(NewsArticle v){return new NewsView(v.id(),v.title(),v.summary(),v.link(),v.publisher(),v.publishedAt(),v.collectedAt(),v.category(),v.sentiment(),v.importance(),v.shortReason());}}
}
