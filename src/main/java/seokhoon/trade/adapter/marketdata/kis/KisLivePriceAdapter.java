package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.LivePricePort;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.config.LiveTradingProperties;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@ConditionalOnProperty(
        name = "tradeguard.live-trading.kis-trading-enabled",
        havingValue = "true"
)
public class KisLivePriceAdapter implements LivePricePort {
    private static final String PATH =
            "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String TR_ID = "FHKST01010100";
    private final KisHttpClient client;
    private final KisAccessTokenProvider tokens;
    private final KisProperties kis;
    private final LiveTradingProperties live;

    KisLivePriceAdapter(
            KisHttpClient client,
            KisAccessTokenProvider tokens,
            KisProperties kis,
            LiveTradingProperties live
    ) {
        this.client = client;
        this.tokens = tokens;
        this.kis = kis;
        this.live = live;
    }

    @Override
    public BigDecimal getCurrentPrice(String stockCode) {
        live.validateOrderEnabled();
        String query = "FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD="
                + URLEncoder.encode(stockCode, StandardCharsets.UTF_8);
        KisHttpResponse response = client.get(
                URI.create(kis.baseUrl(kis.getEnvironment()) + PATH + "?" + query),
                Map.of(
                        "authorization", "Bearer "
                                + tokens.getAccessToken(kis.getEnvironment()),
                        "appkey", kis.appKey(kis.getEnvironment()),
                        "appsecret", kis.appSecret(kis.getEnvironment()),
                        "tr_id", TR_ID,
                        "custtype", "P"
                )
        );
        if (response.statusCode() != 200
                || !"0".equals(response.body().path("rt_cd").asText())) {
            throw new KisApiException("KIS live current price request failed");
        }
        JsonNode price = response.body().path("output").path("stck_prpr");
        try {
            return new BigDecimal(price.asText());
        } catch (NumberFormatException exception) {
            throw new KisApiException(
                    "KIS live current price response is invalid",
                    exception
            );
        }
    }
}
