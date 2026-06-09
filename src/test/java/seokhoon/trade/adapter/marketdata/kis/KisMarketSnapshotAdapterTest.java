package seokhoon.trade.adapter.marketdata.kis;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import seokhoon.trade.adapter.metrics.MicrometerOperationalMetricsAdapter;
import seokhoon.trade.application.port.out.IntradayMarketSnapshot;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KisMarketSnapshotAdapterTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void mapsReadOnlyCurrentPriceResponseToIntradaySnapshot() {
        RecordingKisHttpClient httpClient = new RecordingKisHttpClient(json("""
                {
                  "rt_cd":"0",
                  "output":{
                    "stck_prpr":"76000",
                    "prdy_ctrt":"4.50",
                    "stck_hgpr":"77000",
                    "stck_lwpr":"72000",
                    "acml_vol":"8500000",
                    "acml_tr_pbmn":"65000000000",
                    "wghn_avrg_stck_prc":"74500"
                  }
                }
                """));
        KisProperties properties = properties();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        KisMarketSnapshotAdapter adapter = new KisMarketSnapshotAdapter(
                httpClient,
                tokenProvider(httpClient, properties),
                properties,
                new MicrometerOperationalMetricsAdapter(registry),
                Clock.fixed(Instant.parse("2026-06-05T06:00:00Z"), ZoneOffset.UTC)
        );

        IntradayMarketSnapshot snapshot = adapter.getSnapshot("005930").orElseThrow();

        assertThat(snapshot.stockCode()).isEqualTo("005930");
        assertThat(snapshot.currentPrice()).isEqualByComparingTo("76000");
        assertThat(snapshot.changeRate()).isEqualByComparingTo("4.50");
        assertThat(snapshot.intradayHigh()).isEqualByComparingTo("77000");
        assertThat(snapshot.intradayLow()).isEqualByComparingTo("72000");
        assertThat(snapshot.accumulatedVolume()).isEqualTo(8_500_000);
        assertThat(snapshot.accumulatedTradingValue()).isEqualByComparingTo("65000000000");
        assertThat(snapshot.vwap()).isEqualByComparingTo("74500");
        assertThat(snapshot.snapshotTime()).isEqualTo(Instant.parse("2026-06-05T06:00:00Z"));
        assertThat(httpClient.lastGetUri.getPath())
                .isEqualTo("/uapi/domestic-stock/v1/quotations/inquire-price");
        assertThat(httpClient.lastGetUri.getQuery())
                .contains("FID_INPUT_ISCD=005930");
        assertThat(httpClient.lastGetHeaders)
                .containsEntry("tr_id", "FHKST01010100");
        assertThat(httpClient.lastGetUri.toString()).doesNotContain("order");
        assertThat(registry.find("tradeguard.kis.read_only.count")
                .tag("operation", "currentPrice")
                .tag("result", "success")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .noneMatch(tag -> tag.getValue().contains("005930"))
                .noneMatch(tag -> tag.getValue().contains("test-app-key"))
                .noneMatch(tag -> tag.getValue().contains("test-app-secret"));
    }

    @Test
    void mapsBlankWeightedAveragePriceToNullVwap() {
        RecordingKisHttpClient httpClient = new RecordingKisHttpClient(json("""
                {
                  "rt_cd":"0",
                  "output":{
                    "stck_prpr":"76000",
                    "prdy_ctrt":"4.50",
                    "stck_hgpr":"77000",
                    "stck_lwpr":"72000",
                    "acml_vol":"8500000",
                    "acml_tr_pbmn":"65000000000",
                    "wghn_avrg_stck_prc":""
                  }
                }
                """));
        KisProperties properties = properties();
        KisMarketSnapshotAdapter adapter = new KisMarketSnapshotAdapter(
                httpClient,
                tokenProvider(httpClient, properties),
                properties,
                Clock.systemUTC()
        );

        IntradayMarketSnapshot snapshot = adapter.getSnapshot("005930").orElseThrow();

        assertThat(snapshot.vwap()).isNull();
    }

    private static KisAccessTokenProvider tokenProvider(
            RecordingKisHttpClient httpClient,
            KisProperties properties
    ) {
        return new KisAccessTokenProvider(
                httpClient,
                properties,
                Clock.fixed(Instant.parse("2026-06-05T05:00:00Z"), ZoneOffset.UTC)
        );
    }

    private static KisProperties properties() {
        KisProperties properties = new KisProperties();
        properties.setAppKey("test-app-key");
        properties.setAppSecret("test-app-secret");
        return properties;
    }

    private static JsonNode json(String value) {
        try {
            return OBJECT_MAPPER.readTree(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    private static class RecordingKisHttpClient implements KisHttpClient {
        private final JsonNode getResponse;
        private URI lastGetUri;
        private Map<String, String> lastGetHeaders = Map.of();

        private RecordingKisHttpClient(JsonNode getResponse) {
            this.getResponse = getResponse;
        }

        @Override
        public KisHttpResponse postJson(URI uri, Map<String, String> headers, Object body) {
            return new KisHttpResponse(200, json("""
                    {"access_token":"test-token","expires_in":86400}
                    """));
        }

        @Override
        public KisHttpResponse get(URI uri, Map<String, String> headers) {
            lastGetUri = uri;
            lastGetHeaders = headers;
            return new KisHttpResponse(200, getResponse);
        }
    }
}
