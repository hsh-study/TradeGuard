package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.application.port.out.StockOrderBookPort;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class KisStockOrderBookAdapter implements StockOrderBookPort {
    private static final String PATH = "/uapi/domestic-stock/v1/quotations/inquire-asking-price-exp-ccn";
    private static final String TR_ID = "FHKST01010200";
    private final KisHttpClient client;
    private final KisAccessTokenProvider tokens;
    private final KisProperties kis;
    private final TradingAccountManagementUseCase accounts;

    KisStockOrderBookAdapter(KisHttpClient client, KisAccessTokenProvider tokens,
            KisProperties kis, TradingAccountManagementUseCase accounts) {
        this.client = client; this.tokens = tokens; this.kis = kis; this.accounts = accounts;
    }

    @Override
    public Snapshot load(String stockCode, long accountId) {
        var account = accounts.credentials(accountId)
                .orElseThrow(() -> new IllegalArgumentException("active trading account not found"));
        var environment = account.environment();
        String query = "FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD="
                + URLEncoder.encode(stockCode, StandardCharsets.UTF_8);
        KisHttpResponse response = client.get(URI.create(kis.baseUrl(environment) + PATH + "?" + query),
                Map.of("authorization", "Bearer " + tokens.getAccessToken(environment),
                        "appkey", kis.appKey(environment), "appsecret", kis.appSecret(environment),
                        "tr_id", TR_ID, "custtype", "P"));
        if (response.statusCode() != 200 || !"0".equals(response.body().path("rt_cd").asText())) {
            throw new KisApiException("KIS order book inquiry failed");
        }
        JsonNode output = response.body().path("output1");
        List<Level> asks = new ArrayList<>(), bids = new ArrayList<>();
        for (int level = 1; level <= 10; level++) {
            BigDecimal ask = decimal(output, "askp" + level);
            BigDecimal bid = decimal(output, "bidp" + level);
            if (ask != null && ask.signum() > 0) asks.add(new Level(level, ask, integer(output, "askp_rsqn" + level)));
            if (bid != null && bid.signum() > 0) bids.add(new Level(level, bid, integer(output, "bidp_rsqn" + level)));
        }
        BigDecimal current = decimal(response.body().path("output2"), "antc_cnpr");
        if (current == null) current = decimal(output, "stck_prpr");
        return new Snapshot(stockCode, current, List.copyOf(asks), List.copyOf(bids), Instant.now());
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        try { String value = node.path(field).asText(""); return value.isBlank() ? null : new BigDecimal(value); }
        catch (NumberFormatException exception) { return null; }
    }
    private static long integer(JsonNode node, String field) {
        try { return Long.parseLong(node.path(field).asText("0")); }
        catch (NumberFormatException exception) { return 0; }
    }
}
