package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import seokhoon.trade.domain.stock.Market;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "KIS_SMOKE_TEST_ENABLED", matches = "true")
class KisMarketRankingSmokeTest {
    @Test
    void callsReadOnlyKisMarketRankingApi() {
        KisProperties properties = propertiesFromEnvironment();
        KisHttpClient httpClient = new JdkKisHttpClient(new ObjectMapper());
        KisMarketRankingAdapter adapter = new KisMarketRankingAdapter(
                httpClient,
                new KisAccessTokenProvider(httpClient, properties),
                properties
        );

        var stocks = adapter.findTopTradingValueStocks(Market.KOSPI, 1);

        assertThat(stocks).isNotNull();
        assertThat(stocks).hasSizeLessThanOrEqualTo(1);
    }

    private static KisProperties propertiesFromEnvironment() {
        String appKey = System.getenv("KIS_APP_KEY");
        String appSecret = System.getenv("KIS_APP_SECRET");
        Assumptions.assumeTrue(appKey != null && !appKey.isBlank(), "KIS_APP_KEY is not configured");
        Assumptions.assumeTrue(
                appSecret != null && !appSecret.isBlank(),
                "KIS_APP_SECRET is not configured"
        );

        KisProperties properties = new KisProperties();
        properties.setBaseUrl(System.getenv().getOrDefault(
                "KIS_BASE_URL",
                "https://openapivts.koreainvestment.com:29443"
        ));
        properties.setAppKey(appKey);
        properties.setAppSecret(appSecret);
        return properties;
    }
}
