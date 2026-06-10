package seokhoon.trade.adapter.marketdata.kis;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import seokhoon.trade.adapter.metrics.MicrometerOperationalMetricsAdapter;
import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.IntradayBar;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KisIntradayBarAdapterTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-10T00:31:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void mapsReadOnlyIntradayResponseAndCalculatesPerMinuteTradingValueAndVwap() {
        RecordingKisHttpClient httpClient = new RecordingKisHttpClient(
                response(200, """
                        {
                          "rt_cd":"0",
                          "output2":[
                            {
                              "stck_bsop_date":"20260610",
                              "stck_cntg_hour":"090200",
                              "stck_oprc":"102",
                              "stck_hgpr":"106",
                              "stck_lwpr":"101",
                              "stck_prpr":"105",
                              "cntg_vol":"200",
                              "acml_tr_pbmn":"31000"
                            },
                            {
                              "stck_bsop_date":"20260610",
                              "stck_cntg_hour":"090100",
                              "stck_oprc":"100",
                              "stck_hgpr":"103",
                              "stck_lwpr":"99",
                              "stck_prpr":"102",
                              "cntg_vol":"100",
                              "acml_tr_pbmn":"10000"
                            },
                            {
                              "stck_bsop_date":"20260610",
                              "stck_cntg_hour":"090000",
                              "stck_oprc":"99",
                              "stck_hgpr":"100",
                              "stck_lwpr":"99",
                              "stck_prpr":"100",
                              "cntg_vol":"0",
                              "acml_tr_pbmn":"0"
                            }
                          ]
                        }
                        """)
        );
        KisProperties properties = properties();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        KisIntradayBarAdapter adapter = new KisIntradayBarAdapter(
                httpClient,
                tokenProvider(httpClient, properties),
                properties,
                new MicrometerOperationalMetricsAdapter(registry),
                CLOCK
        );

        List<IntradayBar> bars = adapter.findBars(
                "005930",
                TRADE_DATE,
                LocalTime.of(9, 1),
                LocalTime.of(9, 2),
                BarInterval.ONE_MINUTE
        );

        assertThat(bars).hasSize(2);
        assertThat(bars.get(0)).satisfies(bar -> {
            assertThat(bar.barTime()).isEqualTo(LocalTime.of(9, 1));
            assertThat(bar.openPrice()).isEqualByComparingTo("100");
            assertThat(bar.highPrice()).isEqualByComparingTo("103");
            assertThat(bar.lowPrice()).isEqualByComparingTo("99");
            assertThat(bar.closePrice()).isEqualByComparingTo("102");
            assertThat(bar.volume()).isEqualTo(100);
            assertThat(bar.tradingValue()).isEqualByComparingTo("10000");
            assertThat(bar.vwap()).isEqualByComparingTo("100.0000");
        });
        assertThat(bars.get(1).tradingValue()).isEqualByComparingTo("21000");
        assertThat(bars.get(1).vwap()).isEqualByComparingTo("105.0000");
        assertThat(httpClient.lastGetUri.getPath())
                .isEqualTo(KisIntradayBarAdapter.INTRADAY_BAR_PATH);
        assertThat(httpClient.lastGetUri.getQuery())
                .contains("FID_INPUT_ISCD=005930")
                .contains("FID_INPUT_HOUR_1=090200")
                .contains("FID_PW_DATA_INCU_YN=Y");
        assertThat(httpClient.lastGetHeaders)
                .containsEntry("tr_id", "FHKST03010200");
        assertThat(httpClient.lastGetUri.toString()).doesNotContain("order");
        assertThat(registry.find("tradeguard.kis.read_only.count")
                .tag("operation", "intradayBar")
                .tag("result", "success")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .noneMatch(tag -> tag.getValue().contains("005930"))
                .noneMatch(tag -> tag.getValue().contains("test-app-key"))
                .noneMatch(tag -> tag.getValue().contains("test-app-secret"));
    }

    @Test
    void aggregatesOneMinuteBarsIntoFiveMinuteOhlcvBars() {
        List<IntradayBar> aggregated =
                KisIntradayBarAdapter.aggregateFiveMinuteBars(List.of(
                        bar("09:00", "100", "103", "99", "102", 100, "10000"),
                        bar("09:01", "102", "106", "101", "105", 200, "21000"),
                        bar("09:05", "105", "108", "104", "107", 300, "32100")
                ));

        assertThat(aggregated).hasSize(2);
        assertThat(aggregated.get(0)).satisfies(bar -> {
            assertThat(bar.barTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(bar.openPrice()).isEqualByComparingTo("100");
            assertThat(bar.highPrice()).isEqualByComparingTo("106");
            assertThat(bar.lowPrice()).isEqualByComparingTo("99");
            assertThat(bar.closePrice()).isEqualByComparingTo("105");
            assertThat(bar.volume()).isEqualTo(300);
            assertThat(bar.tradingValue()).isEqualByComparingTo("31000");
            assertThat(bar.vwap()).isEqualByComparingTo("103.3333");
        });
        assertThat(aggregated.get(1).barTime()).isEqualTo(LocalTime.of(9, 5));
    }

    @Test
    void recordsFailureMetricWhenReadOnlyRequestFails() {
        RecordingKisHttpClient httpClient = new RecordingKisHttpClient(
                response(500, "{}")
        );
        KisProperties properties = properties();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        KisIntradayBarAdapter adapter = new KisIntradayBarAdapter(
                httpClient,
                tokenProvider(httpClient, properties),
                properties,
                new MicrometerOperationalMetricsAdapter(registry),
                CLOCK
        );

        assertThatThrownBy(() -> adapter.findBars(
                "005930",
                TRADE_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                BarInterval.ONE_MINUTE
        )).isInstanceOf(KisApiException.class);
        assertThat(registry.find("tradeguard.kis.read_only.count")
                .tag("operation", "intradayBar")
                .tag("result", "failure")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void returnsEmptyWithoutCallingKisForPastTradeDate() {
        RecordingKisHttpClient httpClient = new RecordingKisHttpClient(
                response(200, "{}")
        );
        KisProperties properties = properties();
        KisIntradayBarAdapter adapter = new KisIntradayBarAdapter(
                httpClient,
                tokenProvider(httpClient, properties),
                properties,
                new MicrometerOperationalMetricsAdapter(
                        new SimpleMeterRegistry()
                ),
                CLOCK
        );

        assertThat(adapter.findBars(
                "005930",
                TRADE_DATE.minusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                BarInterval.ONE_MINUTE
        )).isEmpty();
        assertThat(httpClient.lastGetUri).isNull();
    }

    private static IntradayBar bar(
            String time,
            String open,
            String high,
            String low,
            String close,
            long volume,
            String tradingValue
    ) {
        BigDecimal value = new BigDecimal(tradingValue);
        return new IntradayBar(
                "005930",
                TRADE_DATE,
                LocalTime.parse(time),
                new BigDecimal(open),
                new BigDecimal(high),
                new BigDecimal(low),
                new BigDecimal(close),
                volume,
                value,
                value.divide(
                        BigDecimal.valueOf(volume),
                        4,
                        java.math.RoundingMode.HALF_UP
                )
        );
    }

    private static KisAccessTokenProvider tokenProvider(
            RecordingKisHttpClient httpClient,
            KisProperties properties
    ) {
        return new KisAccessTokenProvider(httpClient, properties, CLOCK);
    }

    private static KisProperties properties() {
        KisProperties properties = new KisProperties();
        properties.setAppKey("test-app-key");
        properties.setAppSecret("test-app-secret");
        return properties;
    }

    private static KisHttpResponse response(int status, String body) {
        return new KisHttpResponse(status, json(body));
    }

    private static JsonNode json(String value) {
        try {
            return OBJECT_MAPPER.readTree(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    private static class RecordingKisHttpClient implements KisHttpClient {
        private final KisHttpResponse getResponse;
        private URI lastGetUri;
        private Map<String, String> lastGetHeaders = Map.of();

        private RecordingKisHttpClient(KisHttpResponse getResponse) {
            this.getResponse = getResponse;
        }

        @Override
        public KisHttpResponse postJson(
                URI uri,
                Map<String, String> headers,
                Object body
        ) {
            return response(
                    200,
                    """
                    {"access_token":"test-token","expires_in":86400}
                    """
            );
        }

        @Override
        public KisHttpResponse get(URI uri, Map<String, String> headers) {
            lastGetUri = uri;
            lastGetHeaders = headers;
            return getResponse;
        }
    }
}
