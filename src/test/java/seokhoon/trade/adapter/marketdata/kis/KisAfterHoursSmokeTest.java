package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(
        named = "KIS_AFTER_HOURS_SMOKE_TEST_ENABLED",
        matches = "true"
)
class KisAfterHoursSmokeTest {
    @Test
    void callsReadOnlyKisDailyAfterHoursApi() {
        KisProperties properties = propertiesFromEnvironment();
        KisHttpClient httpClient = new JdkKisHttpClient(new ObjectMapper());
        KisAfterHoursMarketDataAdapter adapter = new KisAfterHoursMarketDataAdapter(
                httpClient,
                new KisAccessTokenProvider(httpClient, properties),
                properties,
                OperationalMetricsPort.noop(),
                Clock.systemUTC()
        );
        String stockCode = System.getenv().getOrDefault(
                "KIS_AFTER_HOURS_SMOKE_TEST_STOCK_CODE",
                "005930"
        );
        LocalDate tradeDate = previousWeekday(
                LocalDate.now(ZoneId.of("Asia/Seoul"))
        );

        var quote = adapter.findByStockCode(stockCode, tradeDate);

        assertThat(quote).isPresent();
        assertThat(quote.orElseThrow().stockCode()).isEqualTo(stockCode);
    }

    private static LocalDate previousWeekday(LocalDate date) {
        LocalDate previous = date.minusDays(1);
        while (previous.getDayOfWeek() == DayOfWeek.SATURDAY
                || previous.getDayOfWeek() == DayOfWeek.SUNDAY) {
            previous = previous.minusDays(1);
        }
        return previous;
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
