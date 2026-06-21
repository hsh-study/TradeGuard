package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.LiveTradingProperties;
import seokhoon.trade.domain.kis.KisEnvironment;
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
    private static final String CANCEL_PATH="/uapi/domestic-stock/v1/trading/order-rvsecncl";
    private static final String CANCELABLE_PATH="/uapi/domestic-stock/v1/trading/inquire-psbl-rvsecncl";
    private final KisHttpClient client;
    private final KisAccessTokenProvider tokens;
    private final KisProperties kis;
    private final LiveTradingProperties live;
    private final TradingAccountManagementUseCase accounts;

    @Autowired
    KisLiveTradingOrderAdapter(KisHttpClient client,KisAccessTokenProvider tokens,
            KisProperties kis,LiveTradingProperties live,
            TradingAccountManagementUseCase accounts){this.client=client;this.tokens=tokens;this.kis=kis;this.live=live;this.accounts=accounts;}

    KisLiveTradingOrderAdapter(KisHttpClient client,KisAccessTokenProvider tokens,
            KisProperties kis,LiveTradingProperties live){this(client,tokens,kis,live,null);}

    @Override public LiveOrderSubmission submitBuyLimitOrder(LiveOrderRequest order){return submit(order,true);}
    @Override public LiveOrderSubmission submitSellLimitOrder(LiveOrderRequest order){return submit(order,false);}

    private LiveOrderSubmission submit(LiveOrderRequest order,boolean buy){
        live.validateOrderEnabled();
        TradingAccountManagementUseCase.AccountCredentials account=account();
        String trId=trId(buy);
        Map<String,String> headers=headers(trId);
        Map<String,String> body=Map.of(
                "CANO",account.accountNumber(),
                "ACNT_PRDT_CD",account.productCode(),
                "PDNO",order.stockCode(),
                "ORD_DVSN","00",
                "ORD_QTY",Integer.toString(order.quantity()),
                "ORD_UNPR",order.orderPrice().stripTrailingZeros().toPlainString(),
                "EXCG_ID_DVSN_CD","KRX",
                "SLL_TYPE","00",
                "CNDT_PRIC",""
        );
        KisHttpResponse response=client.postJson(URI.create(baseUrl()+ORDER_PATH),headers,body);
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
        TradingAccountManagementUseCase.AccountCredentials account=account();
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Seoul"));
        String query=params(Map.ofEntries(
                Map.entry("CANO",account.accountNumber()),Map.entry("ACNT_PRDT_CD",account.productCode()),
                Map.entry("INQR_STRT_DT",today.format(DateTimeFormatter.BASIC_ISO_DATE)),Map.entry("INQR_END_DT",today.format(DateTimeFormatter.BASIC_ISO_DATE)),
                Map.entry("SLL_BUY_DVSN_CD","00"),Map.entry("PDNO",""),Map.entry("CCLD_DVSN","01"),
                Map.entry("INQR_DVSN","00"),Map.entry("INQR_DVSN_3","01"),Map.entry("ORD_GNO_BRNO",""),
                Map.entry("ODNO",""),Map.entry("INQR_DVSN_1",""),Map.entry("CTX_AREA_FK100",""),
                Map.entry("CTX_AREA_NK100",""),Map.entry("EXCG_ID_DVSN_CD","KRX")
        ));
        String trId=isReal()?"TTTC0081R":"VTTC0081R";
        KisHttpResponse response=client.get(URI.create(
                baseUrl()+INQUIRY_PATH+"?"+query),headers(trId));
        if(response.statusCode()!=200||!"0".equals(response.body().path("rt_cd").asText()))throw new KisApiException("KIS fill inquiry failed");
        Map<String,LiveOrderRequest> byNo=new HashMap<>();orders.forEach(o->byNo.put(o.kisOrderNo(),o));
        List<LiveTradeFill> result=new ArrayList<>();
        for(JsonNode row:response.body().path("output1")){
            LiveOrderRequest order=byNo.get(row.path("odno").asText());
            if(order==null)continue;
            int qty=integer(row,"tot_ccld_qty");if(qty<=0)continue;
            BigDecimal price=averageFilledPrice(row);
            if(price==null)continue;
            BigDecimal amount=price.multiply(BigDecimal.valueOf(qty));
            result.add(new LiveTradeFill(null,order.id(),order.stockCode(),order.side(),qty,price,amount,BigDecimal.ZERO,BigDecimal.ZERO,Instant.now()));
        }
        return List.copyOf(result);
    }

    @Override
    public LiveOrderCancellation cancelOrder(LiveOrderRequest order,
            int cancelQuantity, boolean cancelAll) {
        live.validateKisAccessEnabled();
        TradingAccountManagementUseCase.AccountCredentials account=account();
        if (isReal()) {
            int possible = inquireCancelableQuantity(order);
            if (cancelQuantity > possible) {
                return LiveOrderCancellation.rejected(
                        "KIS_CANCEL_QUANTITY_EXCEEDS_POSSIBLE");
            }
        }
        Map<String,String> body = new LinkedHashMap<>();
        body.put("CANO",account.accountNumber());
        body.put("ACNT_PRDT_CD",account.productCode());
        body.put("KRX_FWDG_ORD_ORGNO",
                Objects.toString(order.kisOriginalOrderNo(),""));
        body.put("ORGN_ODNO",order.kisOrderNo());
        body.put("ORD_DVSN","00");
        body.put("RVSE_CNCL_DVSN_CD","02");
        body.put("ORD_QTY",Integer.toString(cancelQuantity));
        body.put("ORD_UNPR","0");
        body.put("QTY_ALL_ORD_YN",cancelAll?"Y":"N");
        body.put("EXCG_ID_DVSN_CD","KRX");
        body.put("CNDT_PRIC","");
        String trId=isReal()?"TTTC0013U":"VTTC0013U";
        KisHttpResponse response=client.postJson(
                URI.create(baseUrl()+CANCEL_PATH),
                headers(trId),body);
        if(response.statusCode()!=200) {
            return LiveOrderCancellation.rejected(
                    "KIS_HTTP_"+response.statusCode());
        }
        if(!"0".equals(response.body().path("rt_cd").asText())) {
            return LiveOrderCancellation.rejected(error(response.body()));
        }
        String cancelNo=response.body().path("output").path("ODNO").asText("");
        if(cancelNo.isBlank()) {
            return LiveOrderCancellation.rejected(
                    "KIS_CANCEL_ORDER_NUMBER_MISSING");
        }
        return LiveOrderCancellation.accepted(cancelNo);
    }

    @Override
    public List<LiveOpenOrderSnapshot> inquireOpenOrders(
            List<LiveOrderRequest> orders) {
        if(orders.isEmpty()) return List.of();
        TradingAccountManagementUseCase.AccountCredentials account=account();
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Seoul"));
        String query=params(Map.ofEntries(
                Map.entry("CANO",account.accountNumber()),
                Map.entry("ACNT_PRDT_CD",account.productCode()),
                Map.entry("INQR_STRT_DT",today.format(DateTimeFormatter.BASIC_ISO_DATE)),
                Map.entry("INQR_END_DT",today.format(DateTimeFormatter.BASIC_ISO_DATE)),
                Map.entry("SLL_BUY_DVSN_CD","00"),Map.entry("PDNO",""),
                Map.entry("CCLD_DVSN","00"),Map.entry("INQR_DVSN","00"),
                Map.entry("INQR_DVSN_3","01"),Map.entry("ORD_GNO_BRNO",""),
                Map.entry("ODNO",""),Map.entry("INQR_DVSN_1",""),
                Map.entry("CTX_AREA_FK100",""),Map.entry("CTX_AREA_NK100",""),
                Map.entry("EXCG_ID_DVSN_CD","KRX")
        ));
        String trId=isReal()?"TTTC0081R":"VTTC0081R";
        KisHttpResponse response=client.get(URI.create(
                baseUrl()+INQUIRY_PATH+"?"+query),headers(trId));
        if(response.statusCode()!=200
                ||!"0".equals(response.body().path("rt_cd").asText())) {
            throw new KisApiException("KIS open order inquiry failed");
        }
        Map<String,LiveOrderRequest> byNo=new HashMap<>();
        orders.forEach(order->byNo.put(order.kisOrderNo(),order));
        Instant now=Instant.now();
        List<LiveOpenOrderSnapshot> result=new ArrayList<>();
        for(JsonNode row:response.body().path("output1")) {
            LiveOrderRequest order=byNo.get(row.path("odno").asText());
            if(order==null) continue;
            int filled=integer(row,"tot_ccld_qty");
            int remaining=integer(row,"rmn_qty");
            if(remaining==0 && filled<order.quantity()) {
                remaining=order.quantity()-filled;
            }
            result.add(new LiveOpenOrderSnapshot(order.id(),filled,remaining,
                    averageFilledPrice(row),now));
        }
        return List.copyOf(result);
    }

    @Override
    public LiveOpenOrderSnapshot inquireOrderDetail(LiveOrderRequest order) {
        return inquireOpenOrders(List.of(order)).stream().findFirst()
                .orElse(new LiveOpenOrderSnapshot(order.id(),
                        order.filledQuantity(),order.remainingQuantity(),
                        null,Instant.now()));
    }

    private int inquireCancelableQuantity(LiveOrderRequest order) {
        TradingAccountManagementUseCase.AccountCredentials account=account();
        String query=params(Map.of(
                "CANO",account.accountNumber(),
                "ACNT_PRDT_CD",account.productCode(),
                "INQR_DVSN_1","0","INQR_DVSN_2","0",
                "CTX_AREA_FK100","","CTX_AREA_NK100",""
        ));
        KisHttpResponse response=client.get(URI.create(
                baseUrl()+CANCELABLE_PATH+"?"+query),
                headers("TTTC0084R"));
        if(response.statusCode()!=200
                ||!"0".equals(response.body().path("rt_cd").asText())) {
            throw new KisApiException("KIS cancelable order inquiry failed");
        }
        for(JsonNode row:response.body().path("output")) {
            if(order.kisOrderNo().equals(row.path("odno").asText())) {
                return integer(row,"psbl_qty");
            }
        }
        return 0;
    }

    private Map<String,String> headers(String trId){var environment=account().environment();return Map.of("authorization","Bearer "+tokens.getAccessToken(environment),"appkey",kis.appKey(environment),"appsecret",kis.appSecret(environment),"tr_id",trId,"custtype","P");}
    private TradingAccountManagementUseCase.AccountCredentials account(){
        if(accounts!=null)return accounts.primaryCredentials()
                .orElseThrow(()->new IllegalStateException("primary DB trading account is not configured"));
        if(live.getAccountNumber()==null||live.getAccountNumber().isBlank()
                ||live.getAccountProductCode()==null||live.getAccountProductCode().isBlank())
            throw new IllegalStateException("trading account is not configured");
        return new TradingAccountManagementUseCase.AccountCredentials(null,"legacy",live.environment(),
                live.getAccountNumber(),live.getAccountProductCode());
    }
    private String baseUrl(){return kis.baseUrl(account().environment());}
    private String trId(boolean buy){return isReal()?(buy?"TTTC0012U":"TTTC0011U"):(buy?"VTTC0012U":"VTTC0011U");}
    private boolean isReal(){return account().environment()==KisEnvironment.REAL;}
    private static String error(JsonNode body){String code=body.path("msg_cd").asText("UNKNOWN");return ("KIS_"+code).substring(0,Math.min(1000,("KIS_"+code).length()));}
    private static int integer(JsonNode n,String f){try{return Integer.parseInt(n.path(f).asText("0"));}catch(NumberFormatException e){return 0;}}
    private static BigDecimal decimal(JsonNode n,String f){try{String v=n.path(f).asText("");return v.isBlank()?null:new BigDecimal(v);}catch(NumberFormatException e){return null;}}
    private static BigDecimal averageFilledPrice(JsonNode row) {
        BigDecimal value=decimal(row,"ccld_avg_pric");
        if(value==null)value=decimal(row,"ord_avg_pric");
        if(value==null)value=decimal(row,"avg_prvs");
        return value;
    }
    private static String params(Map<String,String> values){return values.entrySet().stream().map(e->e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).reduce((a,b)->a+"&"+b).orElse("");}
}
