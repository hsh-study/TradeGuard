package seokhoon.trade.adapter.marketdata.kis;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import seokhoon.trade.adapter.metrics.MicrometerOperationalMetricsAdapter;
import seokhoon.trade.domain.market.AfterHoursQuote;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KisAfterHoursMarketDataAdapterTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 9);
    private static final Instant CAPTURED_AT =
            Instant.parse("2026-06-10T00:30:00Z");

    @Test
    void mapsRequestedTradeDateFromReadOnlyDailyAfterHoursResponse() {
        RecordingKisHttpClient httpClient = new RecordingKisHttpClient(
                response(200, """
                        {
                          "rt_cd":"0",
                          "output1":{},
                          "output2":[
                            {
                              "stck_bsop_date":"20260610",
                              "ovtm_untp_prpr":"76500",
                              "ovtm_untp_prdy_ctrt":"1.20",
                              "ovtm_untp_vol":"100000",
                              "ovtm_untp_tr_pbmn":"7650000000"
                            },
                            {
                              "stck_bsop_date":"20260609",
                              "ovtm_untp_prpr":"76000",
                              "ovtm_untp_prdy_ctrt":"3.50",
                              "ovtm_untp_vol":"850000",
                              "ovtm_untp_tr_pbmn":"42000000000"
                            }
                          ]
                        }
                        """)
        );
        KisProperties properties = properties();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        KisAfterHoursMarketDataAdapter adapter = adapter(
                httpClient,
                properties,
                registry
        );

        AfterHoursQuote quote = adapter.findByStockCode("005930", TRADE_DATE)
                .orElseThrow();

        assertThat(quote.stockCode()).isEqualTo("005930");
        assertThat(quote.stockName()).isEqualTo("005930");
        assertThat(quote.tradeDate()).isEqualTo(TRADE_DATE);
        assertThat(quote.afterHoursPrice()).isEqualByComparingTo("76000");
        assertThat(quote.afterHoursChangeRate()).isEqualByComparingTo("3.50");
        assertThat(quote.afterHoursVolume()).isEqualTo(850_000);
        assertThat(quote.afterHoursTradingValue())
                .isEqualByComparingTo("42000000000");
        assertThat(quote.capturedAt()).isEqualTo(CAPTURED_AT);
        assertThat(httpClient.lastGetUri.getPath())
                .isEqualTo(KisAfterHoursMarketDataAdapter.DAILY_AFTER_HOURS_PATH);
        assertThat(httpClient.lastGetUri.getQuery())
                .contains("FID_INPUT_ISCD=005930");
        assertThat(httpClient.lastGetHeaders)
                .containsEntry("tr_id", "FHPST02320000");
        assertThat(httpClient.lastGetUri.toString()).doesNotContain("order");
        assertThat(registry.find("tradeguard.kis.read_only.count")
                .tag("operation", "afterHours")
                .tag("result", "found")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .noneMatch(tag -> tag.getValue().contains("005930"))
                .noneMatch(tag -> tag.getValue().contains("test-app-key"))
                .noneMatch(tag -> tag.getValue().contains("test-app-secret"));
    }

    @Test
    void returnsEmptyAndRecordsNotFoundWhenDateIsOutsideRecentData() {
        RecordingKisHttpClient httpClient = new RecordingKisHttpClient(
                response(200, """
                        {"rt_cd":"0","output1":{},"output2":[]}
                        """)
        );
        KisProperties properties = properties();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        assertThat(adapter(httpClient, properties, registry)
                .findByStockCode("005930", TRADE_DATE)).isEmpty();
        assertThat(registry.find("tradeguard.kis.read_only.count")
                .tag("operation", "afterHours")
                .tag("result", "not_found")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordsFailureMetricWhenReadOnlyRequestFails() {
        RecordingKisHttpClient httpClient = new RecordingKisHttpClient(
                response(500, "{}")
        );
        KisProperties properties = properties();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        assertThatThrownBy(() -> adapter(httpClient, properties, registry)
                .findByStockCode("005930", TRADE_DATE))
                .isInstanceOf(KisApiException.class);
        assertThat(registry.find("tradeguard.kis.read_only.count")
                .tag("operation", "afterHours")
                .tag("result", "failure")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void returnsEmptyForMarketWideRankingBecauseEndpointIsStockSpecific() {
        RecordingKisHttpClient httpClient = new RecordingKisHttpClient(
                response(200, "{}")
        );
        KisProperties properties = properties();

        assertThat(adapter(
                httpClient,
                properties,
                new SimpleMeterRegistry()
        ).findTopAfterHoursMovers(TRADE_DATE, 10)).isEmpty();
        assertThat(httpClient.lastGetUri).isNull();
    }

    private static KisAfterHoursMarketDataAdapter adapter(
            RecordingKisHttpClient httpClient,
            KisProperties properties,
            SimpleMeterRegistry registry
    ) {
        return new KisAfterHoursMarketDataAdapter(
                httpClient,
                new KisAccessTokenProvider(
                        httpClient,
                        properties,
                        Clock.fixed(CAPTURED_AT, ZoneOffset.UTC)
                ),
                properties,
                new MicrometerOperationalMetricsAdapter(registry),
                Clock.fixed(CAPTURED_AT, ZoneOffset.UTC)
        );
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
