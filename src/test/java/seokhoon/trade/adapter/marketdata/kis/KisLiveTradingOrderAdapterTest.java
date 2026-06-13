package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.LiveOrderSubmission;
import seokhoon.trade.config.LiveTradingProperties;
import seokhoon.trade.domain.order.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KisLiveTradingOrderAdapterTest {
    private static final ObjectMapper JSON=new ObjectMapper();

    @Test
    void submitsRealCashLimitBuyWithOfficialTrId() throws Exception {
        RecordingClient client=new RecordingClient(new KisHttpResponse(200,JSON.readTree("""
                {"rt_cd":"0","output":{"ODNO":"12345","KRX_FWDG_ORD_ORGNO":"00000"}}
                """)));
        LiveTradingProperties live=properties("REAL","https://openapi.koreainvestment.com:9443");
        KisProperties kis=new KisProperties();kis.setAppKey("key");kis.setAppSecret("secret");
        LiveKisAccessTokenProvider tokens=mock(LiveKisAccessTokenProvider.class);
        when(tokens.get()).thenReturn("token");
        KisLiveTradingOrderAdapter adapter=new KisLiveTradingOrderAdapter(client,tokens,kis,live);

        LiveOrderSubmission result=adapter.submitBuyLimitOrder(order(OrderSide.BUY));

        assertThat(result.accepted()).isTrue();
        assertThat(result.orderNo()).isEqualTo("12345");
        assertThat(client.uri.getPath()).isEqualTo("/uapi/domestic-stock/v1/trading/order-cash");
        assertThat(client.headers).containsEntry("tr_id","TTTC0012U");
        assertThat(client.body.toString()).contains("ORD_DVSN=00").contains("PDNO=005930");
        assertThat(client.headers.toString()).doesNotContain("ACCOUNT");
    }

    @Test
    void usesOfficialRealSellAndDemoBuyTrIds() throws Exception {
        RecordingClient realClient=client();
        KisProperties kis=new KisProperties();kis.setAppKey("key");kis.setAppSecret("secret");
        LiveKisAccessTokenProvider tokens=mock(LiveKisAccessTokenProvider.class);when(tokens.get()).thenReturn("token");
        new KisLiveTradingOrderAdapter(realClient,tokens,kis,
                properties("REAL","https://openapi.koreainvestment.com:9443"))
                .submitSellLimitOrder(order(OrderSide.SELL));
        assertThat(realClient.headers).containsEntry("tr_id","TTTC0011U");

        RecordingClient demoClient=client();
        new KisLiveTradingOrderAdapter(demoClient,tokens,kis,
                properties("DEMO","https://openapivts.koreainvestment.com:29443"))
                .submitBuyLimitOrder(order(OrderSide.BUY));
        assertThat(demoClient.headers).containsEntry("tr_id","VTTC0012U");
    }

    private static RecordingClient client() throws Exception{return new RecordingClient(new KisHttpResponse(200,JSON.readTree("{\"rt_cd\":\"0\",\"output\":{\"ODNO\":\"1\"}}")));}
    private static LiveTradingProperties properties(String env,String url){LiveTradingProperties p=new LiveTradingProperties();p.setLiveTradingEnabled(true);p.setKisTradingEnabled(true);p.setAccountNumber("ACCOUNT");p.setAccountProductCode("01");p.setKisEnvironment(env);p.setTradingBaseUrl(url);return p;}
    private static LiveOrderRequest order(OrderSide side){return new LiveOrderRequest(1L,null,"005930",side,1,new BigDecimal("70000"),OrderType.LIMIT,LiveOrderStatus.RISK_APPROVED,null,null,null,Instant.now(),null,Instant.now());}

    private static class RecordingClient implements KisHttpClient {
        private final KisHttpResponse response; URI uri; Map<String,String> headers; Object body;
        RecordingClient(KisHttpResponse response){this.response=response;}
        public KisHttpResponse postJson(URI uri,Map<String,String> headers,Object body){this.uri=uri;this.headers=headers;this.body=body;return response;}
        public KisHttpResponse get(URI uri,Map<String,String> headers){this.uri=uri;this.headers=headers;return response;}
    }
}
