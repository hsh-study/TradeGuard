package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.MarketRankingPort;
import seokhoon.trade.application.port.out.MarketRankingStock;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.domain.stock.Market;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "tradeguard.market-data.realtime-provider", havingValue = "kis")
public class KisMarketRankingAdapter implements MarketRankingPort {
    private static final Logger log = LoggerFactory.getLogger(KisMarketRankingAdapter.class);
    private static final String VOLUME_RANK_PATH =
            "/uapi/domestic-stock/v1/quotations/volume-rank";
    private static final String VOLUME_RANK_TR_ID = "FHPST01710000";
    private static final String FLUCTUATION_PATH =
            "/uapi/domestic-stock/v1/ranking/fluctuation";
    private static final String FLUCTUATION_TR_ID = "FHPST01700000";

    private final KisHttpClient httpClient;
    private final KisAccessTokenProvider tokenProvider;
    private final KisProperties properties;
    private final OperationalMetricsPort operationalMetricsPort;

    @Autowired
    public KisMarketRankingAdapter(
            KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties,
            OperationalMetricsPort operationalMetricsPort
    ) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.properties = properties;
        this.operationalMetricsPort = operationalMetricsPort;
    }

    KisMarketRankingAdapter(
            KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties
    ) {
        this(httpClient, tokenProvider, properties, OperationalMetricsPort.noop());
    }

    @Override
    public List<MarketRankingStock> findTopTradingValueStocks(Market market, int limit) {
        return observe(() -> fetchVolumeRank(market, limit, "3"));
    }

    @Override
    public List<MarketRankingStock> findTopRisingStocks(Market market, int limit) {
        return observe(() -> {
            validateLimit(limit);
            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("FID_COND_MRKT_DIV_CODE", "J");
            parameters.put("FID_COND_SCR_DIV_CODE", "20170");
            parameters.put("FID_INPUT_ISCD", marketCode(market));
            parameters.put("FID_RANK_SORT_CLS_CODE", "0");
            parameters.put("FID_INPUT_CNT_1", Integer.toString(limit));
            parameters.put("FID_PRC_CLS_CODE", "0");
            parameters.put("FID_INPUT_PRICE_1", "");
            parameters.put("FID_INPUT_PRICE_2", "");
            parameters.put("FID_VOL_CNT", "");
            parameters.put("FID_TRGT_CLS_CODE", "0");
            parameters.put("FID_TRGT_EXLS_CLS_CODE", "0");
            parameters.put("FID_DIV_CLS_CODE", "0");
            parameters.put("FID_RSFL_RATE1", "");
            parameters.put("FID_RSFL_RATE2", "");
            JsonNode output = request(FLUCTUATION_PATH, FLUCTUATION_TR_ID, parameters);
            return mapRanking(output, market, limit, "stck_shrn_iscd");
        });
    }

    @Override
    public List<MarketRankingStock> findVolumeSurgeStocks(Market market, int limit) {
        return observe(() -> fetchVolumeRank(market, limit, "1"));
    }

    private List<MarketRankingStock> observe(Supplier<List<MarketRankingStock>> request) {
        try {
            List<MarketRankingStock> result = request.get();
            recordResult("success");
            return result;
        } catch (RuntimeException exception) {
            recordResult("failure");
            throw exception;
        }
    }

    private void recordResult(String result) {
        operationalMetricsPort.recordKisReadOnly("ranking", result);
        log.atInfo()
                .addKeyValue("operation", "ranking")
                .addKeyValue("result", result)
                .log("KIS read-only request completed");
    }

    private List<MarketRankingStock> fetchVolumeRank(Market market, int limit, String rankingType) {
        validateLimit(limit);
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("FID_COND_MRKT_DIV_CODE", "J");
        parameters.put("FID_COND_SCR_DIV_CODE", "20171");
        parameters.put("FID_INPUT_ISCD", marketCode(market));
        parameters.put("FID_DIV_CLS_CODE", "0");
        parameters.put("FID_BLNG_CLS_CODE", rankingType);
        parameters.put("FID_TRGT_CLS_CODE", "0");
        parameters.put("FID_TRGT_EXLS_CLS_CODE", "0");
        parameters.put("FID_INPUT_PRICE_1", "");
        parameters.put("FID_INPUT_PRICE_2", "");
        parameters.put("FID_VOL_CNT", "");
        parameters.put("FID_INPUT_DATE_1", "");
        JsonNode output = request(VOLUME_RANK_PATH, VOLUME_RANK_TR_ID, parameters);
        return mapRanking(output, market, limit, "mksc_shrn_iscd");
    }

    private JsonNode request(String path, String trId, Map<String, String> parameters) {
        properties.validateForRequest();
        Map<String, String> headers = Map.of(
                "authorization", "Bearer " + tokenProvider.getAccessToken(
                        properties.getEnvironment()),
                "appkey", properties.getAppKey(),
                "appsecret", properties.getAppSecret(),
                "tr_id", trId,
                "custtype", "P"
        );
        KisHttpResponse response = httpClient.get(buildUri(path, parameters), headers);
        validateResponse(response, path);
        JsonNode output = response.body().path("output");
        if (!output.isArray()) {
            throw new KisApiException("KIS ranking response did not contain output");
        }
        return output;
    }

    private List<MarketRankingStock> mapRanking(
            JsonNode output,
            Market market,
            int limit,
            String stockCodeField
    ) {
        return java.util.stream.StreamSupport.stream(output.spliterator(), false)
                .map(row -> mapStock(row, market, stockCodeField))
                .limit(limit)
                .toList();
    }

    private static MarketRankingStock mapStock(JsonNode row, Market market, String stockCodeField) {
        BigDecimal currentPrice = decimal(row, "stck_prpr");
        long volume = longValue(row, "acml_vol");
        BigDecimal tradingValue = optionalDecimal(row, "acml_tr_pbmn")
                .orElseGet(() -> currentPrice.multiply(BigDecimal.valueOf(volume)));
        return new MarketRankingStock(
                requiredText(row, stockCodeField),
                requiredText(row, "hts_kor_isnm"),
                market,
                currentPrice,
                decimal(row, "prdy_ctrt"),
                tradingValue,
                volume
        );
    }

    private URI buildUri(String path, Map<String, String> parameters) {
        String query = parameters.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElseThrow();
        return URI.create(properties.getBaseUrl() + path + "?" + query);
    }

    private static void validateResponse(KisHttpResponse response, String path) {
        if (response.statusCode() != 200) {
            throw new KisApiException("KIS read-only request failed with HTTP "
                    + response.statusCode() + " for " + path);
        }
        if (!"0".equals(response.body().path("rt_cd").asText())) {
            String code = response.body().path("msg_cd").asText("unknown");
            String message = response.body().path("msg1").asText("unknown");
            throw new KisApiException("KIS read-only request failed: " + code + " " + message);
        }
    }

    private static String marketCode(Market market) {
        return switch (market) {
            case KOSPI -> "0001";
            case KOSDAQ -> "1001";
            case KONEX -> "2001";
            case UNKNOWN -> throw new IllegalArgumentException("UNKNOWN market is not supported by KIS ranking");
        };
    }

    private static void validateLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static BigDecimal decimal(JsonNode row, String field) {
        try {
            return new BigDecimal(requiredText(row, field));
        } catch (NumberFormatException exception) {
            throw new KisApiException("KIS response contained invalid " + field, exception);
        }
    }

    private static java.util.Optional<BigDecimal> optionalDecimal(JsonNode row, String field) {
        JsonNode value = row.path(field);
        if (!value.isValueNode() || value.asText().isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(new BigDecimal(value.asText()));
        } catch (NumberFormatException exception) {
            throw new KisApiException("KIS response contained invalid " + field, exception);
        }
    }

    private static long longValue(JsonNode row, String field) {
        try {
            return Long.parseLong(requiredText(row, field));
        } catch (NumberFormatException exception) {
            throw new KisApiException("KIS response contained invalid " + field, exception);
        }
    }

    private static String requiredText(JsonNode row, String field) {
        JsonNode value = row.path(field);
        if (!value.isValueNode() || value.asText().isBlank()) {
            throw new KisApiException("KIS response did not contain " + field);
        }
        return value.asText();
    }
}
