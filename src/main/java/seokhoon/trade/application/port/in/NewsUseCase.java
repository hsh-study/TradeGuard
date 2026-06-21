package seokhoon.trade.application.port.in;
import seokhoon.trade.domain.research.*; import java.time.Instant; import java.util.List;
public interface NewsUseCase {
    ImportResult importQuery(String query, Integer display);
    ImportResult importStock(String stockCode);
    List<ImportResult> importWatchlist();
    List<NewsArticle> find(String stockCode, Instant from, Instant to);
    List<NewsImportHistory> histories(String query);
    record ImportResult(String query, int fetchedCount, int savedCount, int duplicatedCount,
            NewsImportStatus status, String reason) {}
}
