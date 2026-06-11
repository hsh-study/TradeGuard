package seokhoon.trade.adapter.marketcalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
class JdkKrxMarketCalendarClient implements KrxMarketCalendarClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private final KoreanMarketCalendarProperties properties;
    private final HttpClient httpClient;

    @Autowired
    JdkKrxMarketCalendarClient(KoreanMarketCalendarProperties properties) {
        this(
                properties,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build()
        );
    }

    JdkKrxMarketCalendarClient(
            KoreanMarketCalendarProperties properties,
            HttpClient httpClient
    ) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public String fetchYear(int year) {
        String endpoint = properties.getKrxEndpoint();
        if (endpoint.isBlank()) {
            // TODO Replace this opt-in endpoint with a stable documented KRX calendar API
            // when KRX publishes one that does not depend on browser OTP/session behavior.
            throw new MarketCalendarSyncException(
                    "KRX calendar endpoint is not configured"
            );
        }
        URI uri = URI.create(endpoint.replace("{year}", Integer.toString(year)));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MarketCalendarSyncException(
                        "KRX calendar request failed with status "
                                + response.statusCode()
                );
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MarketCalendarSyncException(
                    "KRX calendar request was interrupted",
                    exception
            );
        } catch (IOException exception) {
            throw new MarketCalendarSyncException(
                    "KRX calendar request failed",
                    exception
            );
        }
    }
}
