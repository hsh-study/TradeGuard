package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.MarketRankingStock;
import seokhoon.trade.domain.stock.Market;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KisMarketRankingAdapterTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void mapsReadOnlyTradingValueRankingResponse() {
        RecordingKisHttpClient httpClient = new RecordingKisHttpClient(json("""
                {
                  "rt_cd":"0",
                  "output":[
                    {
                      "mksc_shrn_iscd":"005930",
                      "hts_kor_isnm":"삼성전자",
                      "stck_prpr":"76000",
                      "prdy_ctrt":"4.50",
                      "acml_vol":"8500000",
                      "acml_tr_pbmn":"65000000000"
                    }
                  ]
                }
                """));
        KisProperties properties = properties();
        KisMarketRankingAdapter adapter = new KisMarketRankingAdapter(
                httpClient,
                tokenProvider(httpClient, properties),
                properties
        );

        List<MarketRankingStock> stocks = adapter.findTopTradingValueStocks(Market.KOSPI, 5);

        assertThat(stocks)
                .singleElement()
                .satisfies(stock -> {
                    assertThat(stock.stockCode()).isEqualTo("005930");
                    assertThat(stock.stockName()).isEqualTo("삼성전자");
                    assertThat(stock.market()).isEqualTo(Market.KOSPI);
                    assertThat(stock.currentPrice()).isEqualByComparingTo("76000");
                    assertThat(stock.changeRate()).isEqualByComparingTo("4.50");
                    assertThat(stock.tradingValue()).isEqualByComparingTo("65000000000");
                    assertThat(stock.volume()).isEqualTo(8_500_000);
                });
        assertThat(httpClient.lastGetUri.getPath())
                .isEqualTo("/uapi/domestic-stock/v1/quotations/volume-rank");
        assertThat(httpClient.lastGetUri.getQuery())
                .contains("FID_INPUT_ISCD=0001")
                .contains("FID_BLNG_CLS_CODE=3");
        assertThat(httpClient.lastGetHeaders)
                .containsEntry("tr_id", "FHPST01710000");
        assertThat(httpClient.lastGetUri.toString())
                .doesNotContain("order", "trading/order");
    }

    @Test
    void mapsRisingRankingAndDerivesTradingValueWhenFieldIsAbsent() {
        RecordingKisHttpClient httpClient = new RecordingKisHttpClient(json("""
                {
                  "rt_cd":"0",
                  "output":[
                    {
                      "stck_shrn_iscd":"247540",
                      "hts_kor_isnm":"에코프로비엠",
                      "stck_prpr":"210000",
                      "prdy_ctrt":"7.20",
                      "acml_vol":"300000"
                    }
                  ]
                }
                """));
        KisProperties properties = properties();
        KisMarketRankingAdapter adapter = new KisMarketRankingAdapter(
                httpClient,
                tokenProvider(httpClient, properties),
                properties
        );

        MarketRankingStock stock = adapter.findTopRisingStocks(Market.KOSDAQ, 5).getFirst();

        assertThat(stock.stockCode()).isEqualTo("247540");
        assertThat(stock.tradingValue()).isEqualByComparingTo("63000000000");
        assertThat(httpClient.lastGetUri.getPath())
                .isEqualTo("/uapi/domestic-stock/v1/ranking/fluctuation");
        assertThat(httpClient.lastGetUri.getQuery())
                .contains("FID_INPUT_ISCD=1001");
        assertThat(httpClient.lastGetHeaders)
                .containsEntry("tr_id", "FHPST01700000");
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
        private final List<URI> postUris = new ArrayList<>();

        private RecordingKisHttpClient(JsonNode getResponse) {
            this.getResponse = getResponse;
        }

        @Override
        public KisHttpResponse postJson(URI uri, Map<String, String> headers, Object body) {
            postUris.add(uri);
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
