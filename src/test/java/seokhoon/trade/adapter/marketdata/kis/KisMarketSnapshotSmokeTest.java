package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "KIS_SMOKE_TEST_ENABLED", matches = "true")
class KisMarketSnapshotSmokeTest {
    @Test
    void callsReadOnlyKisCurrentPriceApi() {
        KisProperties properties = propertiesFromEnvironment();
        KisHttpClient httpClient = new JdkKisHttpClient(new ObjectMapper());
        KisMarketSnapshotAdapter adapter = new KisMarketSnapshotAdapter(
                httpClient,
                new KisAccessTokenProvider(httpClient, properties),
                properties
        );

        var snapshot = adapter.getSnapshot("005930");

        assertThat(snapshot).isPresent();
        assertThat(snapshot.orElseThrow().stockCode()).isEqualTo("005930");
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
