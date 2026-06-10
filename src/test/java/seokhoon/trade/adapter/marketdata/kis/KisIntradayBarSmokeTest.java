package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.market.BarInterval;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(
        named = "KIS_INTRADAY_BAR_SMOKE_TEST_ENABLED",
        matches = "true"
)
class KisIntradayBarSmokeTest {
    @Test
    void callsReadOnlyKisIntradayBarApi() {
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        LocalTime now = LocalTime.now(seoul);
        Assumptions.assumeTrue(
                !now.isBefore(LocalTime.of(9, 0)),
                "KIS intraday bars are not available before 09:00 Asia/Seoul"
        );
        KisProperties properties = propertiesFromEnvironment();
        KisHttpClient httpClient = new JdkKisHttpClient(new ObjectMapper());
        KisIntradayBarAdapter adapter = new KisIntradayBarAdapter(
                httpClient,
                new KisAccessTokenProvider(httpClient, properties),
                properties,
                OperationalMetricsPort.noop(),
                Clock.system(seoul)
        );
        String stockCode = System.getenv().getOrDefault(
                "KIS_INTRADAY_BAR_SMOKE_TEST_STOCK_CODE",
                "005930"
        );

        var bars = adapter.findBars(
                stockCode,
                LocalDate.now(seoul),
                LocalTime.of(9, 0),
                now.isBefore(LocalTime.of(9, 30))
                        ? now.withSecond(0).withNano(0)
                        : LocalTime.of(9, 30),
                BarInterval.ONE_MINUTE
        );

        assertThat(bars).isNotEmpty();
        assertThat(bars).allMatch(bar -> bar.stockCode().equals(stockCode));
    }

    private static KisProperties propertiesFromEnvironment() {
        String appKey = System.getenv("KIS_APP_KEY");
        String appSecret = System.getenv("KIS_APP_SECRET");
        Assumptions.assumeTrue(
                appKey != null && !appKey.isBlank(),
                "KIS_APP_KEY is not configured"
        );
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
