package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.IntradayMarketSnapshot;
import seokhoon.trade.application.port.out.MarketSnapshotPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "tradeguard.market-data.realtime-provider", havingValue = "kis")
public class KisMarketSnapshotAdapter implements MarketSnapshotPort {
    private static final Logger log = LoggerFactory.getLogger(KisMarketSnapshotAdapter.class);
    private static final String CURRENT_PRICE_PATH =
            "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String CURRENT_PRICE_TR_ID = "FHKST01010100";

    private final KisHttpClient httpClient;
    private final KisAccessTokenProvider tokenProvider;
    private final KisProperties properties;
    private final OperationalMetricsPort operationalMetricsPort;
    private final Clock clock;

    @Autowired
    public KisMarketSnapshotAdapter(
            KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties,
            OperationalMetricsPort operationalMetricsPort
    ) {
        this(
                httpClient,
                tokenProvider,
                properties,
                operationalMetricsPort,
                Clock.systemUTC()
        );
    }

    KisMarketSnapshotAdapter(
            KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties
    ) {
        this(
                httpClient,
                tokenProvider,
                properties,
                OperationalMetricsPort.noop(),
                Clock.systemUTC()
        );
    }

    KisMarketSnapshotAdapter(
            KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties,
            Clock clock
    ) {
        this(
                httpClient,
                tokenProvider,
                properties,
                OperationalMetricsPort.noop(),
                clock
        );
    }

    KisMarketSnapshotAdapter(
            KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties,
            OperationalMetricsPort operationalMetricsPort,
            Clock clock
    ) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.properties = properties;
        this.operationalMetricsPort = operationalMetricsPort;
        this.clock = clock;
    }

    @Override
    public Optional<IntradayMarketSnapshot> getSnapshot(String stockCode) {
        try {
            Optional<IntradayMarketSnapshot> snapshot = fetchSnapshot(stockCode);
            recordResult("success");
            return snapshot;
        } catch (RuntimeException exception) {
            recordResult("failure");
            throw exception;
        }
    }

    private Optional<IntradayMarketSnapshot> fetchSnapshot(String stockCode) {
        properties.validateForRequest();
        Map<String, String> headers = Map.of(
                "authorization", "Bearer " + tokenProvider.getAccessToken(
                        properties.getEnvironment()),
                "appkey", properties.getAppKey(),
                "appsecret", properties.getAppSecret(),
                "tr_id", CURRENT_PRICE_TR_ID,
                "custtype", "P"
        );
        KisHttpResponse response = httpClient.get(buildUri(stockCode), headers);
        validateResponse(response);
        JsonNode output = response.body().path("output");
        if (!output.isObject()) {
            return Optional.empty();
        }
        return Optional.of(new IntradayMarketSnapshot(
                stockCode,
                decimal(output, "stck_prpr"),
                decimal(output, "prdy_ctrt"),
                decimal(output, "stck_hgpr"),
                decimal(output, "stck_lwpr"),
                longValue(output, "acml_vol"),
                decimal(output, "acml_tr_pbmn"),
                nullableDecimal(output, "wghn_avrg_stck_prc"),
                clock.instant()
        ));
    }

    private void recordResult(String result) {
        operationalMetricsPort.recordKisReadOnly("currentPrice", result);
        log.atInfo()
                .addKeyValue("operation", "currentPrice")
                .addKeyValue("result", result)
                .log("KIS read-only request completed");
    }

    private URI buildUri(String stockCode) {
        String query = "FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD="
                + URLEncoder.encode(stockCode, StandardCharsets.UTF_8);
        return URI.create(properties.getBaseUrl() + CURRENT_PRICE_PATH + "?" + query);
    }

    private static void validateResponse(KisHttpResponse response) {
        if (response.statusCode() != 200) {
            throw new KisApiException("KIS current price request failed with HTTP " + response.statusCode());
        }
        if (!"0".equals(response.body().path("rt_cd").asText())) {
            String code = response.body().path("msg_cd").asText("unknown");
            String message = response.body().path("msg1").asText("unknown");
            throw new KisApiException("KIS current price request failed: " + code + " " + message);
        }
    }

    private static BigDecimal decimal(JsonNode row, String field) {
        BigDecimal value = nullableDecimal(row, field);
        if (value == null) {
            throw new KisApiException("KIS response did not contain " + field);
        }
        return value;
    }

    private static BigDecimal nullableDecimal(JsonNode row, String field) {
        JsonNode value = row.path(field);
        if (!value.isValueNode() || value.asText().isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException exception) {
            throw new KisApiException("KIS response contained invalid " + field, exception);
        }
    }

    private static long longValue(JsonNode row, String field) {
        try {
            return Long.parseLong(row.path(field).asText());
        } catch (NumberFormatException exception) {
            throw new KisApiException("KIS response contained invalid " + field, exception);
        }
    }
}
