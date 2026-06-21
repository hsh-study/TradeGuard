package seokhoon.trade.application.port.out;
import java.time.Instant; import java.util.List;
public interface NewsProviderPort {
    List<ProviderNews> search(String query, int display);
    record ProviderNews(String title, String summary, String originLink, String link,
            String publisher, Instant publishedAt) {}
}
