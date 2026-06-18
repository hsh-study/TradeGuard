package seokhoon.trade.adapter.marketdata.kis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.InvestorFlowProviderPort;
import seokhoon.trade.application.port.out.InvestorFlowDiagnosticPort;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.config.InvestorFlowProperties;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.market.*;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@ConditionalOnProperty(name = "tradeguard.investor-flow.provider-enabled", havingValue = "true")
@ConditionalOnExpression("'${tradeguard.investor-flow.provider-type:KIS}'.equalsIgnoreCase('KIS')")
public class KisInvestorFlowProviderAdapter implements InvestorFlowProviderPort, InvestorFlowDiagnosticPort {
    static final String STOCK_TR_ID = "FHKST01010900";
    static final String MARKET_TR_ID = "FHPTJ04040000";
    static final String STOCK_PATH = "/uapi/domestic-stock/v1/quotations/inquire-investor";
    static final String MARKET_PATH = "/uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market";
    static final String QUANTITY_UNIT = "SHARE";
    private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Logger log = LoggerFactory.getLogger(KisInvestorFlowProviderAdapter.class);
    private static final List<String> DIAGNOSTIC_AMOUNT_FIELDS = List.of(
            "frgn_ntby_tr_pbmn", "orgn_ntby_tr_pbmn", "prsn_ntby_tr_pbmn",
            "frgn_shnu_tr_pbmn", "orgn_shnu_tr_pbmn", "prsn_shnu_tr_pbmn",
            "frgn_seln_tr_pbmn", "orgn_seln_tr_pbmn", "prsn_seln_tr_pbmn"
    );
    private static final List<String> DIAGNOSTIC_QUANTITY_FIELDS = List.of(
            "frgn_ntby_qty", "orgn_ntby_qty", "prsn_ntby_qty",
            "frgn_shnu_vol", "orgn_shnu_vol", "prsn_shnu_vol",
            "frgn_seln_vol", "orgn_seln_vol", "prsn_seln_vol"
    );

    private final KisHttpClient httpClient;
    private final KisAccessTokenProvider tokenProvider;
    private final KisProperties kisProperties;
    private final InvestorFlowProperties investorFlowProperties;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public KisInvestorFlowProviderAdapter(KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider, KisProperties kisProperties,
            InvestorFlowProperties investorFlowProperties, OperationalMetricsPort metrics) {
        this(httpClient, tokenProvider, kisProperties, investorFlowProperties,
                metrics, Clock.systemUTC());
    }

    KisInvestorFlowProviderAdapter(KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider, KisProperties kisProperties,
            InvestorFlowProperties investorFlowProperties, OperationalMetricsPort metrics,
            Clock clock) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.kisProperties = kisProperties;
        this.investorFlowProperties = investorFlowProperties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    public InvestorFlowFetchResult<StockInvestorFlow> fetchStockInvestorFlows(
            String stockCode, LocalDate tradeDate) {
        validateAmountUnit();
        validateStockCode(stockCode);
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("FID_COND_MRKT_DIV_CODE", "J");
        parameters.put("FID_INPUT_ISCD", stockCode);
        JsonNode output = request(STOCK_PATH, STOCK_TR_ID, parameters, "stock");
        JsonNode row = findByDate(output, tradeDate);
        if (row == null) {
            return InvestorFlowFetchResult.empty();
        }
        return mapStockRow(stockCode, tradeDate, row);
    }

    @Override
    public InvestorFlowFetchResult<MarketInvestorFlow> fetchMarketInvestorFlows(
            InvestorFlowMarket market, LocalDate tradeDate) {
        validateAmountUnit();
        if (kisProperties.getEnvironment() != KisEnvironment.REAL) {
            throw new UnsupportedOperationException(
                    "KIS market investor flow TR is verified for REAL environment only");
        }
        MarketParameters marketParameters = marketParameters(market);
        String date = KIS_DATE.format(tradeDate);
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("FID_COND_MRKT_DIV_CODE", "U");
        parameters.put("FID_INPUT_ISCD", marketParameters.industryCode());
        parameters.put("FID_INPUT_DATE_1", date);
        parameters.put("FID_INPUT_ISCD_1", marketParameters.marketCode());
        parameters.put("FID_INPUT_DATE_2", date);
        parameters.put("FID_INPUT_ISCD_2", marketParameters.industryCode());
        JsonNode output = request(MARKET_PATH, MARKET_TR_ID, parameters, "market");
        JsonNode row = findByDate(output, tradeDate);
        if (row == null) {
            return InvestorFlowFetchResult.empty();
        }
        return mapMarketRow(market, tradeDate, row);
    }

    @Override
    public InvestorFlowDiagnosticData diagnoseStock(String stockCode, LocalDate tradeDate) {
        validateStockCode(stockCode);
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("FID_COND_MRKT_DIV_CODE", "J");
        parameters.put("FID_INPUT_ISCD", stockCode);
        JsonNode output = request(STOCK_PATH, STOCK_TR_ID, parameters, "diagnostic_stock");
        return diagnosticData(STOCK_PATH, STOCK_TR_ID, output, tradeDate);
    }

    @Override
    public InvestorFlowDiagnosticData diagnoseMarket(
            InvestorFlowMarket market, LocalDate tradeDate) {
        if (kisProperties.getEnvironment() != KisEnvironment.REAL) {
            throw new UnsupportedOperationException(
                    "KIS market investor flow diagnostic is verified for REAL environment only");
        }
        MarketParameters marketParameters = marketParameters(market);
        String date = KIS_DATE.format(tradeDate);
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("FID_COND_MRKT_DIV_CODE", "U");
        parameters.put("FID_INPUT_ISCD", marketParameters.industryCode());
        parameters.put("FID_INPUT_DATE_1", date);
        parameters.put("FID_INPUT_ISCD_1", marketParameters.marketCode());
        parameters.put("FID_INPUT_DATE_2", date);
        parameters.put("FID_INPUT_ISCD_2", marketParameters.industryCode());
        JsonNode output = request(MARKET_PATH, MARKET_TR_ID, parameters,
                "diagnostic_market");
        return diagnosticData(MARKET_PATH, MARKET_TR_ID, output, tradeDate);
    }

    private JsonNode request(String path, String trId, Map<String, String> parameters,
            String operation) {
        try {
            kisProperties.validateForRequest();
            Map<String, String> headers = Map.of(
                    "authorization", "Bearer " + tokenProvider.getAccessToken(kisProperties.getEnvironment()),
                    "appkey", kisProperties.getAppKey(),
                    "appsecret", kisProperties.getAppSecret(),
                    "tr_id", trId,
                    "custtype", "P"
            );
            int timeoutSeconds = investorFlowProperties.getProviderTimeoutSeconds();
            if (timeoutSeconds <= 0) {
                throw new IllegalStateException("KIS investor flow timeout must be positive");
            }
            KisHttpResponse response = httpClient.get(buildUri(path, parameters), headers,
                    Duration.ofSeconds(timeoutSeconds));
            validateResponse(response);
            JsonNode output = response.body().path("output");
            if (!output.isArray()) {
                throw new KisApiException("KIS investor flow response did not contain output");
            }
            metrics.recordKisReadOnly("investor_flow_" + operation, "success");
            log.atInfo().addKeyValue("operation", "investor_flow_" + operation)
                    .addKeyValue("result", "success")
                    .log("KIS read-only request completed");
            return output;
        } catch (RuntimeException exception) {
            metrics.recordKisReadOnly("investor_flow_" + operation, "failure");
            log.atWarn().addKeyValue("operation", "investor_flow_" + operation)
                    .addKeyValue("result", "failure")
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("KIS read-only request failed");
            throw exception;
        }
    }

    private InvestorFlowFetchResult<StockInvestorFlow> mapStockRow(
            String stockCode, LocalDate tradeDate, JsonNode row) {
        List<StockInvestorFlow> flows = new ArrayList<>();
        int rejected = 0;
        for (InvestorFields fields : aggregateFields()) {
            try {
                BigDecimal netAmount = amount(row, fields.prefix() + "_ntby_tr_pbmn");
                Long netQuantity = longValue(row, fields.prefix() + "_ntby_qty");
                requireNetValue(netAmount, netQuantity, fields.rawType());
                Instant now = clock.instant();
                flows.add(new StockInvestorFlow(null, stockCode, tradeDate, fields.type(),
                        fields.rawType(), netAmount, netQuantity,
                        amount(row, fields.prefix() + "_shnu_tr_pbmn"),
                        amount(row, fields.prefix() + "_seln_tr_pbmn"),
                        longValue(row, fields.prefix() + "_shnu_vol"),
                        longValue(row, fields.prefix() + "_seln_vol"),
                        InvestorFlowSource.KIS, now, now));
            } catch (RuntimeException exception) {
                rejected++;
            }
        }
        return new InvestorFlowFetchResult<>(flows, rejected);
    }

    private InvestorFlowDiagnosticData diagnosticData(String endpoint, String trId,
            JsonNode output, LocalDate tradeDate) {
        JsonNode requestedRow = findByDate(output, tradeDate);
        JsonNode sample = requestedRow != null
                ? requestedRow : (output.isEmpty() ? null : output.get(0));
        if (sample == null) {
            return new InvestorFlowDiagnosticData(endpoint, trId, 0, List.of(),
                    List.of(), Map.of(), Map.of(), false);
        }
        List<String> available = new ArrayList<>();
        if (optionalText(sample, "stck_bsop_date") != null) {
            available.add("stck_bsop_date");
        }
        DIAGNOSTIC_AMOUNT_FIELDS.stream()
                .filter(field -> optionalText(sample, field) != null)
                .forEach(available::add);
        DIAGNOSTIC_QUANTITY_FIELDS.stream()
                .filter(field -> optionalText(sample, field) != null)
                .forEach(available::add);
        return new InvestorFlowDiagnosticData(
                endpoint,
                trId,
                output.size(),
                available,
                diagnosticInvestorTypes(sample),
                diagnosticValues(sample, DIAGNOSTIC_AMOUNT_FIELDS),
                diagnosticValues(sample, DIAGNOSTIC_QUANTITY_FIELDS),
                requestedRow != null
        );
    }

    private Map<String, String> diagnosticValues(JsonNode sample, List<String> fields) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String field : fields) {
            String value = optionalText(sample, field);
            if (value != null) {
                values.put(field, investorFlowProperties.isDiagnosticMaskResponse()
                        ? maskNumeric(value) : value);
            }
        }
        return values;
    }

    private static List<String> diagnosticInvestorTypes(JsonNode sample) {
        List<String> types = new ArrayList<>();
        if (hasPrefix(sample, "frgn_")) types.add("FOREIGN");
        if (hasPrefix(sample, "orgn_")) types.add("INSTITUTION");
        if (hasPrefix(sample, "prsn_")) types.add("INDIVIDUAL");
        return types;
    }

    private static boolean hasPrefix(JsonNode sample, String prefix) {
        for (String field : DIAGNOSTIC_AMOUNT_FIELDS) {
            if (field.startsWith(prefix) && optionalText(sample, field) != null) return true;
        }
        for (String field : DIAGNOSTIC_QUANTITY_FIELDS) {
            if (field.startsWith(prefix) && optionalText(sample, field) != null) return true;
        }
        return false;
    }

    private static String maskNumeric(String raw) {
        String normalized = raw.replace(",", "").trim();
        try {
            BigDecimal value = new BigDecimal(normalized);
            if (value.signum() == 0) return "ZERO";
            String digits = normalized.replaceFirst("^[+-]", "")
                    .replace(".", "").replaceFirst("^0+", "");
            return (value.signum() < 0 ? "NEGATIVE" : "POSITIVE")
                    + "_DIGITS_" + Math.max(1, digits.length());
        } catch (NumberFormatException exception) {
            return "NON_NUMERIC_MASKED";
        }
    }

    private InvestorFlowFetchResult<MarketInvestorFlow> mapMarketRow(
            InvestorFlowMarket market, LocalDate tradeDate, JsonNode row) {
        List<MarketInvestorFlow> flows = new ArrayList<>();
        int rejected = 0;
        for (InvestorFields fields : aggregateFields()) {
            try {
                BigDecimal netAmount = amount(row, fields.prefix() + "_ntby_tr_pbmn");
                Long netQuantity = longValue(row, fields.prefix() + "_ntby_qty");
                requireNetValue(netAmount, netQuantity, fields.rawType());
                Instant now = clock.instant();
                flows.add(new MarketInvestorFlow(null, market, tradeDate, fields.type(),
                        fields.rawType(), netAmount, netQuantity, null, null,
                        InvestorFlowSource.KIS, now, now));
            } catch (RuntimeException exception) {
                rejected++;
            }
        }
        return new InvestorFlowFetchResult<>(flows, rejected);
    }

    static InvestorType mapInvestorType(String rawType) {
        return switch (rawType.trim()) {
            case "외국인" -> InvestorType.FOREIGN;
            case "기관", "기관계" -> InvestorType.INSTITUTION;
            case "개인" -> InvestorType.INDIVIDUAL;
            case "금융투자", "증권" -> InvestorType.FINANCIAL_INVESTMENT;
            case "보험" -> InvestorType.INSURANCE;
            case "투신", "투자신탁" -> InvestorType.INVESTMENT_TRUST;
            case "은행" -> InvestorType.BANK;
            case "연기금", "연기금등", "기금" -> InvestorType.PENSION_FUND;
            case "사모", "사모펀드" -> InvestorType.PRIVATE_EQUITY;
            case "기타기관", "기타금융" -> InvestorType.OTHER_INSTITUTION;
            default -> InvestorType.ETC;
        };
    }

    private static List<InvestorFields> aggregateFields() {
        return List.of(
                new InvestorFields("frgn", mapInvestorType("외국인"), "외국인"),
                new InvestorFields("orgn", mapInvestorType("기관계"), "기관계"),
                new InvestorFields("prsn", mapInvestorType("개인"), "개인")
        );
    }

    private void validateAmountUnit() {
        investorFlowProperties.getKisAmountUnit().toKrw(BigDecimal.ZERO);
    }

    private BigDecimal amount(JsonNode row, String field) {
        BigDecimal raw = decimal(row, field);
        return raw == null ? null : investorFlowProperties.getKisAmountUnit().toKrw(raw);
    }

    private static BigDecimal decimal(JsonNode row, String field) {
        String value = optionalText(row, field);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException exception) {
            throw new KisApiException("KIS investor flow response contained invalid " + field, exception);
        }
    }

    private static Long longValue(JsonNode row, String field) {
        String value = optionalText(row, field);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.replace(",", ""));
        } catch (NumberFormatException exception) {
            throw new KisApiException("KIS investor flow response contained invalid " + field, exception);
        }
    }

    private static String optionalText(JsonNode row, String field) {
        JsonNode value = row.path(field);
        return !value.isValueNode() || value.asText().isBlank() ? null : value.asText().trim();
    }

    private static void requireNetValue(BigDecimal amount, Long quantity, String rawType) {
        if (amount == null && quantity == null) {
            throw new KisApiException("KIS investor flow response omitted net values for " + rawType);
        }
    }

    private static JsonNode findByDate(JsonNode output, LocalDate tradeDate) {
        String expected = KIS_DATE.format(tradeDate);
        for (JsonNode row : output) {
            if (expected.equals(optionalText(row, "stck_bsop_date"))) {
                return row;
            }
        }
        return null;
    }

    private URI buildUri(String path, Map<String, String> parameters) {
        String query = parameters.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right).orElseThrow();
        return URI.create(kisProperties.getBaseUrl() + path + "?" + query);
    }

    private static void validateResponse(KisHttpResponse response) {
        if (response.statusCode() != 200) {
            throw new KisApiException("KIS investor flow request failed with HTTP " + response.statusCode());
        }
        if (!"0".equals(response.body().path("rt_cd").asText())) {
            String code = response.body().path("msg_cd").asText("unknown");
            throw new KisApiException("KIS investor flow request failed: " + code);
        }
    }

    private static MarketParameters marketParameters(InvestorFlowMarket market) {
        return switch (market) {
            case KOSPI -> new MarketParameters("KSP", "0001");
            case KOSDAQ -> new MarketParameters("KSQ", "1001");
            case KONEX, ALL -> throw new UnsupportedOperationException(
                    "KIS market investor flow is verified for KOSPI and KOSDAQ only");
        };
    }

    private static void validateStockCode(String stockCode) {
        if (stockCode == null || !stockCode.matches("[0-9A-Z_]{6,20}")) {
            throw new IllegalArgumentException("stockCode must be a valid KIS instrument code");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record InvestorFields(String prefix, InvestorType type, String rawType) {}
    private record MarketParameters(String marketCode, String industryCode) {}
}
