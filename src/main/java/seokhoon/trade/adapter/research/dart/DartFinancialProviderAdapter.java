package seokhoon.trade.adapter.research.dart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.DartFinancialProviderPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.config.DartProperties;
import seokhoon.trade.config.DartProviderException;
import seokhoon.trade.domain.research.DartFinancialAccount;
import seokhoon.trade.domain.research.DartFinancialStatement;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DartFinancialProviderAdapter implements DartFinancialProviderPort {
    private static final Logger log = LoggerFactory.getLogger(DartFinancialProviderAdapter.class);
    private static final String FINANCIAL_STATEMENT_PATH = "/api/fnlttSinglAcntAll.json";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DartProperties properties;
    private final OperationalMetricsPort metrics;

    @Autowired
    public DartFinancialProviderAdapter(
            ObjectMapper objectMapper,
            DartProperties properties,
            OperationalMetricsPort metrics
    ) {
        this(HttpClient.newBuilder().connectTimeout(properties.requestTimeout()).build(),
                objectMapper, properties, metrics);
    }

    DartFinancialProviderAdapter(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            DartProperties properties,
            OperationalMetricsPort metrics
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Override
    public DartFinancialStatement fetchFinancialStatement(String corpCode, int fiscalYear, String reportCode) {
        try {
            DartFinancialStatement result = fetch(corpCode, fiscalYear, reportCode);
            record("success");
            return result;
        } catch (RuntimeException exception) {
            record("failure");
            throw exception;
        }
    }

    private DartFinancialStatement fetch(String corpCode, int fiscalYear, String reportCode) {
        properties.validateProviderRequest();
        HttpRequest request = HttpRequest.newBuilder(buildUri(corpCode, fiscalYear, reportCode))
                .timeout(properties.requestTimeout())
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new DartProviderException("DART financial statement request failed with HTTP "
                        + response.statusCode());
            }
            JsonNode body = objectMapper.readTree(response.body());
            String status = body.path("status").asText("");
            if (!"000".equals(status)) {
                throw new DartProviderException("DART financial statement request failed with status " + status);
            }
            JsonNode list = body.path("list");
            if (!list.isArray()) {
                throw new DartProviderException("DART financial statement response did not contain list");
            }
            List<DartFinancialAccount> accounts = new ArrayList<>();
            for (JsonNode row : list) {
                String accountName = row.path("account_nm").asText("");
                if (!accountName.isBlank()) {
                    accounts.add(new DartFinancialAccount(accountName, amount(row.path("thstrm_amount").asText(""))));
                }
            }
            return new DartFinancialStatement(corpCode, fiscalYear, reportCode, accounts);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DartProviderException("DART financial statement request was interrupted", exception);
        } catch (IOException exception) {
            throw new DartProviderException("DART financial statement request failed", exception);
        }
    }

    private URI buildUri(String corpCode, int fiscalYear, String reportCode) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("crtfc_key", properties.getApiKey());
        parameters.put("corp_code", corpCode);
        parameters.put("bsns_year", Integer.toString(fiscalYear));
        parameters.put("reprt_code", reportCode);
        parameters.put("fs_div", "CFS");
        String query = parameters.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElseThrow();
        return URI.create(stripTrailingSlash(properties.getApiBaseUrl()) + FINANCIAL_STATEMENT_PATH + "?" + query);
    }

    private void record(String result) {
        metrics.recordDartProvider("financial_statement", result);
        log.atInfo()
                .addKeyValue("operation", "financial_statement")
                .addKeyValue("result", result)
                .log("DART provider request completed");
    }

    private static BigDecimal amount(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) {
            return null;
        }
        String normalized = value.replace(",", "").trim();
        boolean negative = normalized.startsWith("(") && normalized.endsWith(")");
        if (negative) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        BigDecimal amount = new BigDecimal(normalized);
        return negative ? amount.negate() : amount;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
