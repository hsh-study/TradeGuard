package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import seokhoon.trade.domain.market.DailyPrice;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KisMarketDataAdapterTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void fetchesAndMapsDailyPricesInAscendingDateOrder() {
        FakeKisHttpClient httpClient = new FakeKisHttpClient(
                json("""
                        {"access_token":"test-token","expires_in":86400}
                        """),
                json("""
                        {
                          "rt_cd":"0",
                          "msg_cd":"MCA00000",
                          "output2":[
                            {
                              "stck_bsop_date":"20260605",
                              "stck_oprc":"70000",
                              "stck_hgpr":"72000",
                              "stck_lwpr":"69000",
                              "stck_clpr":"71000",
                              "acml_vol":"1000000",
                              "acml_tr_pbmn":"71000000000"
                            },
                            {
                              "stck_bsop_date":"20260604",
                              "stck_oprc":"68000",
                              "stck_hgpr":"70500",
                              "stck_lwpr":"67500",
                              "stck_clpr":"70000",
                              "acml_vol":"900000",
                              "acml_tr_pbmn":"63000000000"
                            }
                          ]
                        }
                        """)
        );
        KisProperties properties = properties();
        KisAccessTokenProvider tokenProvider = tokenProvider(httpClient, properties);
        KisMarketDataAdapter adapter = new KisMarketDataAdapter(httpClient, tokenProvider, properties);

        List<DailyPrice> prices = adapter.fetchDailyPrices(
                "005930",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 7)
        );

        assertThat(prices).extracting(DailyPrice::tradeDate)
                .containsExactly(LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 5));
        assertThat(prices.getLast().closePrice()).isEqualByComparingTo("71000");
        assertThat(prices.getLast().tradingValue()).isEqualByComparingTo("71000000000");
        assertThat(httpClient.lastGetUri.getQuery())
                .contains("FID_INPUT_ISCD=005930")
                .contains("FID_INPUT_DATE_1=20260601")
                .contains("FID_INPUT_DATE_2=20260607")
                .contains("FID_PERIOD_DIV_CODE=D")
                .contains("FID_ORG_ADJ_PRC=0");
        assertThat(httpClient.lastGetHeaders)
                .containsEntry("authorization", "Bearer test-token")
                .containsEntry("tr_id", "FHKST03010100");
    }

    @Test
    void reusesTokenUntilExpiry() {
        FakeKisHttpClient httpClient = new FakeKisHttpClient(
                json("""
                        {"access_token":"test-token","expires_in":86400}
                        """),
                json("""
                        {"rt_cd":"0","output2":[]}
                        """)
        );
        KisProperties properties = properties();
        KisAccessTokenProvider tokenProvider = tokenProvider(httpClient, properties);
        KisMarketDataAdapter adapter = new KisMarketDataAdapter(httpClient, tokenProvider, properties);

        adapter.fetchDailyPrices("005930", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7));
        adapter.fetchDailyPrices("000660", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7));

        assertThat(httpClient.postCount).isEqualTo(1);
        assertThat(httpClient.getCount).isEqualTo(2);
    }

    @Test
    void rejectsKisBusinessErrorWithoutExposingCredentials() {
        FakeKisHttpClient httpClient = new FakeKisHttpClient(
                json("""
                        {"access_token":"test-token","expires_in":86400}
                        """),
                json("""
                        {"rt_cd":"1","msg_cd":"EGW00123","msg1":"invalid request"}
                        """)
        );
        KisProperties properties = properties();
        KisMarketDataAdapter adapter = new KisMarketDataAdapter(
                httpClient,
                tokenProvider(httpClient, properties),
                properties
        );

        assertThatThrownBy(() -> adapter.fetchDailyPrices(
                "005930",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 7)
        ))
                .isInstanceOf(KisApiException.class)
                .hasMessageContaining("EGW00123")
                .hasMessageNotContaining("test-app-secret");
    }

    @Test
    void blocksNonVirtualInvestmentHost() {
        KisProperties properties = properties();
        properties.setBaseUrl("https://openapi.koreainvestment.com:9443");
        FakeKisHttpClient httpClient = new FakeKisHttpClient(json("{}"), json("{}"));
        KisMarketDataAdapter adapter = new KisMarketDataAdapter(
                httpClient,
                tokenProvider(httpClient, properties),
                properties
        );

        assertThatThrownBy(() -> adapter.fetchDailyPrices(
                "005930",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 7)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only the KIS virtual investment host is allowed");
        assertThat(httpClient.postCount).isZero();
        assertThat(httpClient.getCount).isZero();
    }

    private static KisAccessTokenProvider tokenProvider(FakeKisHttpClient httpClient, KisProperties properties) {
        return new KisAccessTokenProvider(
                httpClient,
                properties,
                Clock.fixed(Instant.parse("2026-06-07T00:00:00Z"), ZoneOffset.UTC)
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

    private static class FakeKisHttpClient implements KisHttpClient {
        private final JsonNode tokenResponse;
        private final JsonNode dailyPriceResponse;
        private int postCount;
        private int getCount;
        private URI lastGetUri;
        private Map<String, String> lastGetHeaders = Map.of();
        private final List<Object> postBodies = new ArrayList<>();

        private FakeKisHttpClient(JsonNode tokenResponse, JsonNode dailyPriceResponse) {
            this.tokenResponse = tokenResponse;
            this.dailyPriceResponse = dailyPriceResponse;
        }

        @Override
        public KisHttpResponse postJson(URI uri, Map<String, String> headers, Object body) {
            postCount++;
            postBodies.add(body);
            return new KisHttpResponse(200, tokenResponse);
        }

        @Override
        public KisHttpResponse get(URI uri, Map<String, String> headers) {
            getCount++;
            lastGetUri = uri;
            lastGetHeaders = headers;
            return new KisHttpResponse(200, dailyPriceResponse);
        }
    }
}
