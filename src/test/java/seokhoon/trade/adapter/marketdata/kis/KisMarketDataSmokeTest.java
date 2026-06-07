package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "KIS_SMOKE_TEST", matches = "true")
class KisMarketDataSmokeTest {
    @Test
    void fetchesSamsungDailyPricesFromVirtualInvestmentApi() {
        KisProperties properties = new KisProperties();
        properties.setBaseUrl(System.getenv().getOrDefault(
                "KIS_BASE_URL",
                "https://openapivts.koreainvestment.com:29443"
        ));
        properties.setAppKey(System.getenv("KIS_APP_KEY"));
        properties.setAppSecret(System.getenv("KIS_APP_SECRET"));
        KisHttpClient httpClient = new JdkKisHttpClient(new ObjectMapper());
        KisAccessTokenProvider tokenProvider = new KisAccessTokenProvider(httpClient, properties);
        KisMarketDataAdapter adapter = new KisMarketDataAdapter(httpClient, tokenProvider, properties);
        LocalDate to = LocalDate.now();

        var prices = adapter.fetchDailyPrices("005930", to.minusDays(120), to);

        assertThat(prices).isNotEmpty();
        assertThat(prices).allSatisfy(price -> assertThat(price.stockCode()).isEqualTo("005930"));
        assertThat(prices).isSortedAccordingTo(
                java.util.Comparator.comparing(seokhoon.trade.domain.market.DailyPrice::tradeDate)
        );
    }
}
