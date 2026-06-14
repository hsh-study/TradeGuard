package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.MarketDataPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.domain.market.DailyPrice;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class KisMarketDataAdapter implements MarketDataPort {
    private static final Logger log = LoggerFactory.getLogger(KisMarketDataAdapter.class);
    private static final String DAILY_PRICE_PATH =
            "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
    private static final String DAILY_PRICE_TR_ID = "FHKST03010100";
    private static final int MAX_RESPONSE_COUNT = 100;
    private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final KisHttpClient httpClient;
    private final KisAccessTokenProvider tokenProvider;
    private final KisProperties properties;
    private final OperationalMetricsPort operationalMetricsPort;

    @Autowired
    public KisMarketDataAdapter(
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

    KisMarketDataAdapter(
            KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties
    ) {
        this(httpClient, tokenProvider, properties, OperationalMetricsPort.noop());
    }

    @Override
    public List<DailyPrice> fetchDailyPrices(String stockCode, LocalDate from, LocalDate to) {
        try {
            List<DailyPrice> prices = fetch(stockCode, from, to);
            recordResult("success");
            return prices;
        } catch (RuntimeException exception) {
            recordResult("failure");
            throw exception;
        }
    }

    private List<DailyPrice> fetch(String stockCode, LocalDate from, LocalDate to) {
        properties.validateForRequest();
        Map<String, String> headers = Map.of(
                "authorization", "Bearer " + tokenProvider.getAccessToken(
                        properties.getEnvironment()),
                "appkey", properties.getAppKey(),
                "appsecret", properties.getAppSecret(),
                "tr_id", DAILY_PRICE_TR_ID,
                "custtype", "P"
        );
        Map<LocalDate, DailyPrice> pricesByDate = new TreeMap<>();
        LocalDate pageTo = to;

        while (!pageTo.isBefore(from)) {
            KisHttpResponse response = httpClient.get(buildUri(stockCode, from, pageTo), headers);
            validateResponse(response);
            List<DailyPrice> page = mapPrices(stockCode, response.body().path("output2")).stream()
                    .filter(price -> !price.tradeDate().isBefore(from) && !price.tradeDate().isAfter(to))
                    .toList();
            page.forEach(price -> pricesByDate.put(price.tradeDate(), price));

            if (page.size() < MAX_RESPONSE_COUNT || page.isEmpty()) {
                break;
            }

            LocalDate oldestDate = page.getFirst().tradeDate();
            if (!oldestDate.isAfter(from)) {
                break;
            }
            LocalDate nextPageTo = oldestDate.minusDays(1);
            if (!nextPageTo.isBefore(pageTo)) {
                throw new KisApiException("KIS daily price pagination did not advance");
            }
            pageTo = nextPageTo;
        }
        return List.copyOf(pricesByDate.values());
    }

    private void recordResult(String result) {
        operationalMetricsPort.recordKisReadOnly("dailyPrice", result);
        log.atInfo()
                .addKeyValue("operation", "dailyPrice")
                .addKeyValue("result", result)
                .log("KIS read-only request completed");
    }

    private URI buildUri(String stockCode, LocalDate from, LocalDate to) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("FID_COND_MRKT_DIV_CODE", "J");
        parameters.put("FID_INPUT_ISCD", stockCode);
        parameters.put("FID_INPUT_DATE_1", KIS_DATE.format(from));
        parameters.put("FID_INPUT_DATE_2", KIS_DATE.format(to));
        parameters.put("FID_PERIOD_DIV_CODE", "D");
        parameters.put("FID_ORG_ADJ_PRC", "0");
        String query = parameters.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElseThrow();
        return URI.create(properties.getBaseUrl() + DAILY_PRICE_PATH + "?" + query);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void validateResponse(KisHttpResponse response) {
        if (response.statusCode() != 200) {
            throw new KisApiException("KIS daily price request failed with HTTP " + response.statusCode());
        }
        if (!"0".equals(response.body().path("rt_cd").asText())) {
            String code = response.body().path("msg_cd").asText("unknown");
            String message = response.body().path("msg1").asText("unknown");
            throw new KisApiException("KIS daily price request failed: " + code + " " + message);
        }
    }

    private static List<DailyPrice> mapPrices(String stockCode, JsonNode output) {
        if (!output.isArray()) {
            throw new KisApiException("KIS daily price response did not contain output2");
        }
        List<DailyPrice> prices = new ArrayList<>();
        for (JsonNode row : output) {
            prices.add(new DailyPrice(
                    stockCode,
                    LocalDate.parse(requiredText(row, "stck_bsop_date"), KIS_DATE),
                    decimal(row, "stck_oprc"),
                    decimal(row, "stck_hgpr"),
                    decimal(row, "stck_lwpr"),
                    decimal(row, "stck_clpr"),
                    longValue(row, "acml_vol"),
                    decimal(row, "acml_tr_pbmn")
            ));
        }
        return prices.stream()
                .sorted(Comparator.comparing(DailyPrice::tradeDate))
                .toList();
    }

    private static BigDecimal decimal(JsonNode row, String field) {
        try {
            return new BigDecimal(requiredText(row, field));
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
