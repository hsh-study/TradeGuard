package seokhoon.trade.adapter.marketdata.kis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import seokhoon.trade.adapter.marketdata.ConditionalOnAfterHoursProvider;
import seokhoon.trade.application.port.out.AfterHoursMarketDataPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.market.AfterHoursQuote;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnAfterHoursProvider("kis")
public class KisAfterHoursMarketDataAdapter implements AfterHoursMarketDataPort {
    static final String DAILY_AFTER_HOURS_PATH =
            "/uapi/domestic-stock/v1/quotations/inquire-daily-overtimeprice";
    static final String DAILY_AFTER_HOURS_TR_ID = "FHPST02320000";

    private static final Logger log =
            LoggerFactory.getLogger(KisAfterHoursMarketDataAdapter.class);
    private static final DateTimeFormatter KIS_DATE =
            DateTimeFormatter.BASIC_ISO_DATE;

    private final KisHttpClient httpClient;
    private final KisAccessTokenProvider tokenProvider;
    private final KisProperties properties;
    private final OperationalMetricsPort metricsPort;
    private final Clock clock;

    @Autowired
    public KisAfterHoursMarketDataAdapter(
            KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties,
            OperationalMetricsPort metricsPort
    ) {
        this(
                httpClient,
                tokenProvider,
                properties,
                metricsPort,
                Clock.systemUTC()
        );
    }

    KisAfterHoursMarketDataAdapter(
            KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties,
            OperationalMetricsPort metricsPort,
            Clock clock
    ) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.properties = properties;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Override
    public List<AfterHoursQuote> findTopAfterHoursMovers(
            LocalDate tradeDate,
            int limit
    ) {
        if (tradeDate == null) {
            throw new IllegalArgumentException("tradeDate must not be null");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        // This KIS endpoint is stock-specific and cannot rank the whole market.
        return List.of();
    }

    @Override
    public Optional<AfterHoursQuote> findByStockCode(
            String stockCode,
            LocalDate tradeDate
    ) {
        validate(stockCode, tradeDate);
        try {
            Optional<AfterHoursQuote> quote = fetch(stockCode, tradeDate);
            recordResult(quote.isPresent() ? "found" : "not_found");
            return quote;
        } catch (RuntimeException exception) {
            recordResult("failure");
            throw exception;
        }
    }

    private Optional<AfterHoursQuote> fetch(
            String stockCode,
            LocalDate tradeDate
    ) {
        properties.validateForRequest();
        Map<String, String> headers = Map.of(
                "authorization", "Bearer " + tokenProvider.getAccessToken(),
                "appkey", properties.getAppKey(),
                "appsecret", properties.getAppSecret(),
                "tr_id", DAILY_AFTER_HOURS_TR_ID,
                "custtype", "P"
        );
        KisHttpResponse response = httpClient.get(buildUri(stockCode), headers);
        validateResponse(response);
        JsonNode output = response.body().path("output2");
        if (!output.isArray()) {
            throw new KisApiException(
                    "KIS after-hours response did not contain output2"
            );
        }
        for (JsonNode row : output) {
            LocalDate rowDate = LocalDate.parse(
                    requiredText(row, "stck_bsop_date"),
                    KIS_DATE
            );
            if (rowDate.equals(tradeDate)) {
                return Optional.of(new AfterHoursQuote(
                        stockCode,
                        stockCode,
                        tradeDate,
                        decimal(row, "ovtm_untp_prpr"),
                        decimal(row, "ovtm_untp_prdy_ctrt"),
                        longValue(row, "ovtm_untp_vol"),
                        decimal(row, "ovtm_untp_tr_pbmn"),
                        clock.instant()
                ));
            }
        }
        return Optional.empty();
    }

    private URI buildUri(String stockCode) {
        String query = "FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD="
                + URLEncoder.encode(stockCode, StandardCharsets.UTF_8);
        return URI.create(
                properties.getBaseUrl() + DAILY_AFTER_HOURS_PATH + "?" + query
        );
    }

    private void recordResult(String result) {
        metricsPort.recordKisReadOnly("afterHours", result);
        log.atInfo()
                .addKeyValue("operation", "afterHours")
                .addKeyValue("provider", "kis")
                .addKeyValue("result", result)
                .log("KIS read-only request completed");
    }

    private static void validate(String stockCode, LocalDate tradeDate) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        if (tradeDate == null) {
            throw new IllegalArgumentException("tradeDate must not be null");
        }
    }

    private static void validateResponse(KisHttpResponse response) {
        if (response.statusCode() != 200) {
            throw new KisApiException(
                    "KIS after-hours request failed with HTTP "
                            + response.statusCode()
            );
        }
        if (!"0".equals(response.body().path("rt_cd").asText())) {
            String code = response.body().path("msg_cd").asText("unknown");
            String message = response.body().path("msg1").asText("unknown");
            throw new KisApiException(
                    "KIS after-hours request failed: " + code + " " + message
            );
        }
    }

    private static BigDecimal decimal(JsonNode row, String field) {
        try {
            return new BigDecimal(requiredText(row, field));
        } catch (NumberFormatException exception) {
            throw new KisApiException(
                    "KIS response contained invalid " + field,
                    exception
            );
        }
    }

    private static long longValue(JsonNode row, String field) {
        try {
            return Long.parseLong(requiredText(row, field));
        } catch (NumberFormatException exception) {
            throw new KisApiException(
                    "KIS response contained invalid " + field,
                    exception
            );
        }
    }

    private static String requiredText(JsonNode row, String field) {
        JsonNode value = row.path(field);
        if (!value.isValueNode() || value.asText().isBlank()) {
            throw new KisApiException(
                    "KIS response did not contain " + field
            );
        }
        return value.asText();
    }
}
