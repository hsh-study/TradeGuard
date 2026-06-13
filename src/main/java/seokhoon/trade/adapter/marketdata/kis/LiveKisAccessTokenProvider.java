package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.stereotype.Component;
import seokhoon.trade.config.LiveTradingProperties;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.time.*;
import java.util.Map;

@Component
class LiveKisAccessTokenProvider {
    private final KisHttpClient client;
    private final KisProperties kis;
    private final LiveTradingProperties live;
    private String token;
    private Instant expiresAt=Instant.EPOCH;
    LiveKisAccessTokenProvider(KisHttpClient client,KisProperties kis,LiveTradingProperties live){this.client=client;this.kis=kis;this.live=live;}
    synchronized String get(){
        live.validateOrderEnabled();
        if(kis.getAppKey()==null||kis.getAppKey().isBlank()||kis.getAppSecret()==null||kis.getAppSecret().isBlank())throw new KisApiException("KIS trading credentials are not configured");
        if(token!=null&&Instant.now().isBefore(expiresAt.minusSeconds(60)))return token;
        KisHttpResponse response=client.postJson(URI.create(live.getTradingBaseUrl()+"/oauth2/tokenP"),Map.of(),Map.of("grant_type","client_credentials","appkey",kis.getAppKey(),"appsecret",kis.getAppSecret()));
        if(response.statusCode()!=200)throw new KisApiException("KIS live token request failed with HTTP "+response.statusCode());
        JsonNode node=response.body().path("access_token");long seconds=response.body().path("expires_in").asLong(0);
        if(!node.isTextual()||node.asText().isBlank()||seconds<=0)throw new KisApiException("KIS live token response is invalid");
        token=node.asText();expiresAt=Instant.now().plusSeconds(seconds);return token;
    }
}
