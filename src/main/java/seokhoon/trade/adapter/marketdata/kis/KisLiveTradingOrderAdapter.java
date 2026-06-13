package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.LiveTradingProperties;
import seokhoon.trade.domain.order.*;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@ConditionalOnProperty(name="tradeguard.live-trading.kis-trading-enabled",havingValue="true")
public class KisLiveTradingOrderAdapter implements LiveTradingOrderPort {
    private static final String ORDER_PATH="/uapi/domestic-stock/v1/trading/order-cash";
    private static final String INQUIRY_PATH="/uapi/domestic-stock/v1/trading/inquire-daily-ccld";
    private final KisHttpClient client;
    private final LiveKisAccessTokenProvider tokens;
    private final KisProperties kis;
    private final LiveTradingProperties live;

    KisLiveTradingOrderAdapter(KisHttpClient client,LiveKisAccessTokenProvider tokens,
            KisProperties kis,LiveTradingProperties live){this.client=client;this.tokens=tokens;this.kis=kis;this.live=live;}

    @Override public LiveOrderSubmission submitBuyLimitOrder(LiveOrderRequest order){return submit(order,true);}
    @Override public LiveOrderSubmission submitSellLimitOrder(LiveOrderRequest order){return submit(order,false);}

    private LiveOrderSubmission submit(LiveOrderRequest order,boolean buy){
        live.validateOrderEnabled();
        String trId=trId(buy);
        Map<String,String> headers=headers(trId);
        Map<String,String> body=Map.of(
                "CANO",live.getAccountNumber(),
                "ACNT_PRDT_CD",live.getAccountProductCode(),
                "PDNO",order.stockCode(),
                "ORD_DVSN","00",
                "ORD_QTY",Integer.toString(order.quantity()),
                "ORD_UNPR",order.orderPrice().stripTrailingZeros().toPlainString(),
                "EXCG_ID_DVSN_CD","KRX",
                "SLL_TYPE","00",
                "CNDT_PRIC",""
        );
        KisHttpResponse response=client.postJson(URI.create(live.getTradingBaseUrl()+ORDER_PATH),headers,body);
        if(response.statusCode()!=200)return LiveOrderSubmission.rejected("KIS_HTTP_"+response.statusCode());
        if(!"0".equals(response.body().path("rt_cd").asText()))return LiveOrderSubmission.rejected(error(response.body()));
        JsonNode output=response.body().path("output");
        String orderNo=output.path("ODNO").asText("");
        if(orderNo.isBlank())return LiveOrderSubmission.rejected("KIS_ORDER_NUMBER_MISSING");
        return LiveOrderSubmission.accepted(orderNo,output.path("KRX_FWDG_ORD_ORGNO").asText(null));
    }

    @Override public LiveOrderSubmission inquireOrder(LiveOrderRequest order){
        inquireFilledOrders(List.of(order));
        return LiveOrderSubmission.accepted(order.kisOrderNo(),order.kisOriginalOrderNo());
    }

    @Override public List<LiveTradeFill> inquireFilledOrders(List<LiveOrderRequest> orders){
        if(orders.isEmpty())return List.of();
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Seoul"));
        String query=params(Map.ofEntries(
                Map.entry("CANO",live.getAccountNumber()),Map.entry("ACNT_PRDT_CD",live.getAccountProductCode()),
                Map.entry("INQR_STRT_DT",today.format(DateTimeFormatter.BASIC_ISO_DATE)),Map.entry("INQR_END_DT",today.format(DateTimeFormatter.BASIC_ISO_DATE)),
                Map.entry("SLL_BUY_DVSN_CD","00"),Map.entry("PDNO",""),Map.entry("CCLD_DVSN","01"),
                Map.entry("INQR_DVSN","00"),Map.entry("INQR_DVSN_3","01"),Map.entry("ORD_GNO_BRNO",""),
                Map.entry("ODNO",""),Map.entry("INQR_DVSN_1",""),Map.entry("CTX_AREA_FK100",""),
                Map.entry("CTX_AREA_NK100",""),Map.entry("EXCG_ID_DVSN_CD","KRX")
        ));
        String trId=isReal()?"TTTC0081R":"VTTC0081R";
        KisHttpResponse response=client.get(URI.create(live.getTradingBaseUrl()+INQUIRY_PATH+"?"+query),headers(trId));
        if(response.statusCode()!=200||!"0".equals(response.body().path("rt_cd").asText()))throw new KisApiException("KIS fill inquiry failed");
        Map<String,LiveOrderRequest> byNo=new HashMap<>();orders.forEach(o->byNo.put(o.kisOrderNo(),o));
        List<LiveTradeFill> result=new ArrayList<>();
        for(JsonNode row:response.body().path("output1")){
            LiveOrderRequest order=byNo.get(row.path("odno").asText());
            if(order==null)continue;
            int qty=integer(row,"tot_ccld_qty");if(qty<=0)continue;
            BigDecimal price=decimal(row,"avg_prvs");if(price==null)price=decimal(row,"avg_prvs");
            if(price==null)continue;
            BigDecimal amount=price.multiply(BigDecimal.valueOf(qty));
            result.add(new LiveTradeFill(null,order.id(),order.stockCode(),order.side(),qty,price,amount,BigDecimal.ZERO,BigDecimal.ZERO,Instant.now()));
        }
        return List.copyOf(result);
    }

    private Map<String,String> headers(String trId){return Map.of("authorization","Bearer "+tokens.get(),"appkey",kis.getAppKey(),"appsecret",kis.getAppSecret(),"tr_id",trId,"custtype","P");}
    private String trId(boolean buy){return isReal()?(buy?"TTTC0012U":"TTTC0011U"):(buy?"VTTC0012U":"VTTC0011U");}
    private boolean isReal(){return "REAL".equalsIgnoreCase(live.getKisEnvironment());}
    private static String error(JsonNode body){String code=body.path("msg_cd").asText("UNKNOWN");return ("KIS_"+code).substring(0,Math.min(1000,("KIS_"+code).length()));}
    private static int integer(JsonNode n,String f){try{return Integer.parseInt(n.path(f).asText("0"));}catch(NumberFormatException e){return 0;}}
    private static BigDecimal decimal(JsonNode n,String f){try{String v=n.path(f).asText("");return v.isBlank()?null:new BigDecimal(v);}catch(NumberFormatException e){return null;}}
    private static String params(Map<String,String> values){return values.entrySet().stream().map(e->e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).reduce((a,b)->a+"&"+b).orElse("");}
}
