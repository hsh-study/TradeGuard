package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import seokhoon.trade.config.LiveTradingProperties;
import seokhoon.trade.domain.order.*;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class KisLiveTradingOrderSmokeTest {
    @Test
    void submitsExplicitlyOptedInDemoLimitBuy() {
        Assumptions.assumeTrue(Boolean.parseBoolean(
                System.getenv("KIS_LIVE_ORDER_SMOKE_TEST_ENABLED")));
        Assumptions.assumeTrue("DEMO".equalsIgnoreCase(
                System.getenv("KIS_TRADING_ENVIRONMENT")),
                "Live order smoke test is restricted to DEMO");

        String appKey=require("KIS_APP_KEY");
        String appSecret=require("KIS_APP_SECRET");
        String account=require("KIS_ACCOUNT_NUMBER");
        String product=require("KIS_ACCOUNT_PRODUCT_CODE");
        String stock=require("KIS_LIVE_ORDER_SMOKE_TEST_STOCK_CODE");
        BigDecimal price=new BigDecimal(require("KIS_LIVE_ORDER_SMOKE_TEST_PRICE"));

        KisHttpClient client=new JdkKisHttpClient(new ObjectMapper());
        KisProperties kis=new KisProperties();
        kis.setAppKey(appKey);kis.setAppSecret(appSecret);
        LiveTradingProperties live=new LiveTradingProperties();
        live.setLiveTradingEnabled(true);live.setKisTradingEnabled(true);
        live.setKisEnvironment("DEMO");
        live.setTradingBaseUrl("https://openapivts.koreainvestment.com:29443");
        live.setAccountNumber(account);live.setAccountProductCode(product);
        KisLiveTradingOrderAdapter adapter=new KisLiveTradingOrderAdapter(
                client,new LiveKisAccessTokenProvider(client,kis,live),kis,live);

        var result=adapter.submitBuyLimitOrder(new LiveOrderRequest(
                1L,null,stock,OrderSide.BUY,1,price,OrderType.LIMIT,
                LiveOrderStatus.RISK_APPROVED,null,null,null,
                Instant.now(),null,Instant.now()));

        assertThat(result.accepted()).isTrue();
        assertThat(result.orderNo()).isNotBlank();
    }

    private static String require(String name){
        String value=System.getenv(name);
        Assumptions.assumeTrue(value!=null&&!value.isBlank(),name+" is required");
        return value;
    }
}
