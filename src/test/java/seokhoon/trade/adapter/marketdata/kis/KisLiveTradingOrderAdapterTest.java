package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.LiveOrderSubmission;
import seokhoon.trade.application.port.out.LiveOrderCancellation;
import seokhoon.trade.config.LiveTradingProperties;
import seokhoon.trade.domain.order.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        when(tokens.getAccessToken(any())).thenReturn("token");
        KisLiveTradingOrderAdapter adapter=new KisLiveTradingOrderAdapter(client,tokens,kis,live);

        LiveOrderSubmission result=adapter.submitBuyLimitOrder(order(OrderSide.BUY));

        assertThat(result.accepted()).isTrue();
        assertThat(result.orderNo()).isEqualTo("12345");
        assertThat(client.uri.getPath()).isEqualTo("/uapi/domestic-stock/v1/trading/order-cash");
        assertThat(client.headers).containsEntry("tr_id","TTTC0012U");
        assertThat(client.body.toString()).contains("ORD_DVSN=00").contains("PDNO=005930");
        assertThat(client.headers.toString()).doesNotContain("ACCOUNT");
        verify(tokens).getAccessToken(
                seokhoon.trade.domain.kis.KisEnvironment.REAL);
    }

    @Test
    void usesOfficialRealSellAndDemoBuyTrIds() throws Exception {
        RecordingClient realClient=client();
        KisProperties kis=new KisProperties();kis.setAppKey("key");kis.setAppSecret("secret");
        LiveKisAccessTokenProvider tokens=mock(LiveKisAccessTokenProvider.class);when(tokens.getAccessToken(any())).thenReturn("token");
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

    @Test
    void submitsDemoCancellationWithOfficialTrId() throws Exception {
        RecordingClient client=client();
        KisProperties kis=new KisProperties();kis.setAppKey("key");
        kis.setAppSecret("secret");
        LiveKisAccessTokenProvider tokens=mock(
                LiveKisAccessTokenProvider.class);
        when(tokens.getAccessToken(any())).thenReturn("token");
        KisLiveTradingOrderAdapter adapter=new KisLiveTradingOrderAdapter(
                client,tokens,kis,properties("DEMO",
                "https://openapivts.koreainvestment.com:29443"));
        LiveOrderRequest accepted=new LiveOrderRequest(1L,null,"005930",
                OrderSide.BUY,2,new BigDecimal("70000"),OrderType.LIMIT,
                LiveOrderStatus.ACCEPTED,"12345","00000",null,
                Instant.now(),Instant.now(),Instant.now());

        LiveOrderCancellation result=adapter.cancelOrder(accepted,2,true);

        assertThat(result.accepted()).isTrue();
        assertThat(client.uri.getPath()).isEqualTo(
                "/uapi/domestic-stock/v1/trading/order-rvsecncl");
        assertThat(client.headers).containsEntry("tr_id","VTTC0013U");
        assertThat(client.body.toString())
                .contains("RVSE_CNCL_DVSN_CD=02")
                .contains("QTY_ALL_ORD_YN=Y")
                .contains("ORD_DVSN=00");
    }

    @Test
    void checksRealCancelableQuantityBeforeOfficialCancellation()
            throws Exception {
        KisHttpClient client=mock(KisHttpClient.class);
        when(client.get(any(),argThat(headers->
                "TTTC0084R".equals(headers.get("tr_id"))))).thenReturn(
                new KisHttpResponse(200,JSON.readTree("""
                        {"rt_cd":"0","output":[
                          {"odno":"12345","psbl_qty":"2"}
                        ]}
                        """)));
        when(client.postJson(any(),argThat(headers->
                "TTTC0013U".equals(headers.get("tr_id"))),any())).thenReturn(
                new KisHttpResponse(200,JSON.readTree("""
                        {"rt_cd":"0","output":{"ODNO":"54321"}}
                        """)));
        KisProperties kis=new KisProperties();kis.setAppKey("key");
        kis.setAppSecret("secret");
        LiveKisAccessTokenProvider tokens=mock(
                LiveKisAccessTokenProvider.class);
        when(tokens.getAccessToken(any())).thenReturn("token");
        KisLiveTradingOrderAdapter adapter=new KisLiveTradingOrderAdapter(
                client,tokens,kis,properties("REAL",
                "https://openapi.koreainvestment.com:9443"));
        LiveOrderRequest accepted=new LiveOrderRequest(1L,null,"005930",
                OrderSide.BUY,2,new BigDecimal("70000"),OrderType.LIMIT,
                LiveOrderStatus.ACCEPTED,"12345","00000",null,
                Instant.now(),Instant.now(),Instant.now());

        assertThat(adapter.cancelOrder(accepted,2,true).accepted()).isTrue();
        verify(client).get(argThat(uri->uri.getPath().equals(
                "/uapi/domestic-stock/v1/trading/inquire-psbl-rvsecncl")),
                argThat(headers->"TTTC0084R".equals(headers.get("tr_id"))));
        verify(client).postJson(argThat(uri->uri.getPath().equals(
                "/uapi/domestic-stock/v1/trading/order-rvsecncl")),
                argThat(headers->"TTTC0013U".equals(headers.get("tr_id"))),
                any());
    }

    private static RecordingClient client() throws Exception{return new RecordingClient(new KisHttpResponse(200,JSON.readTree("{\"rt_cd\":\"0\",\"output\":{\"ODNO\":\"1\"}}")));}
    private static LiveTradingProperties properties(String env,String url){LiveTradingProperties p=new LiveTradingProperties();p.setLiveTradingEnabled(true);p.setKisTradingEnabled(true);p.setAccountNumber("ACCOUNT");p.setAccountProductCode("01");p.setKisEnvironment(env);return p;}
    private static LiveOrderRequest order(OrderSide side){return new LiveOrderRequest(1L,null,"005930",side,1,new BigDecimal("70000"),OrderType.LIMIT,LiveOrderStatus.RISK_APPROVED,null,null,null,Instant.now(),null,Instant.now());}

    private static class RecordingClient implements KisHttpClient {
        private final KisHttpResponse response; URI uri; Map<String,String> headers; Object body;
        RecordingClient(KisHttpResponse response){this.response=response;}
        public KisHttpResponse postJson(URI uri,Map<String,String> headers,Object body){this.uri=uri;this.headers=headers;this.body=body;return response;}
        public KisHttpResponse get(URI uri,Map<String,String> headers){this.uri=uri;this.headers=headers;return response;}
    }
}
