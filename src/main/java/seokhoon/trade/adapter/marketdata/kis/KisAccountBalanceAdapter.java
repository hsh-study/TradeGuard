package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.application.port.out.KisAccountBalancePort;
import seokhoon.trade.domain.kis.KisEnvironment;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class KisAccountBalanceAdapter implements KisAccountBalancePort {
    private static final String PATH = "/uapi/domestic-stock/v1/trading/inquire-balance";
    private final KisHttpClient client;
    private final KisAccessTokenProvider tokens;
    private final KisProperties kis;
    private final TradingAccountManagementUseCase accounts;

    KisAccountBalanceAdapter(KisHttpClient client, KisAccessTokenProvider tokens,
            KisProperties kis, TradingAccountManagementUseCase accounts) {
        this.client = client; this.tokens = tokens; this.kis = kis; this.accounts = accounts;
    }

    @Override
    public List<AccountHolding> holdings(long accountId) {
        var account = accounts.credentials(accountId)
                .orElseThrow(() -> new IllegalArgumentException("active trading account not found"));
        KisEnvironment environment = account.environment();
        Map<String, String> query = new LinkedHashMap<>();
        query.put("CANO", account.accountNumber());
        query.put("ACNT_PRDT_CD", account.productCode());
        query.put("AFHR_FLPR_YN", "N");
        query.put("OFL_YN", "");
        query.put("INQR_DVSN", "02");
        query.put("UNPR_DVSN", "01");
        query.put("FUND_STTL_ICLD_YN", "N");
        query.put("FNCG_AMT_AUTO_RDPT_YN", "N");
        query.put("PRCS_DVSN", "00");
        query.put("CTX_AREA_FK100", "");
        query.put("CTX_AREA_NK100", "");
        String trId = environment == KisEnvironment.REAL ? "TTTC8434R" : "VTTC8434R";
        Map<String, String> headers = Map.of(
                "authorization", "Bearer " + tokens.getAccessToken(environment),
                "appkey", kis.appKey(environment), "appsecret", kis.appSecret(environment),
                "tr_id", trId, "custtype", "P");
        KisHttpResponse response = client.get(URI.create(kis.baseUrl(environment) + PATH
                + "?" + parameters(query)), headers);
        if (response.statusCode() != 200
                || !"0".equals(response.body().path("rt_cd").asText())) {
            throw new KisApiException("KIS account balance inquiry failed");
        }
        List<AccountHolding> result = new ArrayList<>();
        for (JsonNode row : response.body().path("output1")) {
            int quantity = integer(row, "hldg_qty");
            if (quantity <= 0) continue;
            result.add(new AccountHolding(environment, row.path("pdno").asText(),
                    row.path("prdt_name").asText(row.path("pdno").asText()), quantity,
                    decimal(row, "pchs_avg_pric"), decimal(row, "pchs_amt"),
                    decimal(row, "prpr"), decimal(row, "evlu_amt"),
                    decimal(row, "evlu_pfls_amt"), decimal(row, "evlu_pfls_rt")));
        }
        return List.copyOf(result);
    }

    private static String parameters(Map<String, String> values) {
        return values.entrySet().stream().map(e -> e.getKey() + "="
                + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b).orElse("");
    }
    private static int integer(JsonNode node, String field) {
        try { return Integer.parseInt(node.path(field).asText("0")); }
        catch (NumberFormatException exception) { return 0; }
    }
    private static BigDecimal decimal(JsonNode node, String field) {
        try { String value = node.path(field).asText("");
            return value.isBlank() ? null : new BigDecimal(value); }
        catch (NumberFormatException exception) { return null; }
    }
}
