package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import seokhoon.trade.domain.kis.*;
import tools.jackson.databind.ObjectMapper;

import java.time.*;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KisOAuthTokenClientTest {
    private static final ObjectMapper JSON=new ObjectMapper();

    @Test
    void mapsOfficialTokenResponseWithoutExposingRawToken() throws Exception {
        RecordingClient http=new RecordingClient(new KisHttpResponse(200,
                JSON.readTree("""
                        {"access_token":"secret-token","token_type":"Bearer",
                         "expires_in":86400}
                        """)));
        KisProperties properties=new KisProperties();
        properties.setAppKey("key");
        properties.setAppSecret("secret");
        Instant now=Instant.parse("2026-06-13T00:00:00Z");
        KisOAuthTokenClient client=new KisOAuthTokenClient(http,properties,
                Clock.fixed(now,ZoneOffset.UTC));

        KisAccessToken token=client.issueToken(KisEnvironment.REAL,
                "key","secret");

        assertThat(http.uri.toString()).isEqualTo(
                "https://openapi.koreainvestment.com:9443/oauth2/tokenP");
        assertThat(token.expiresAt()).isEqualTo(now.plusSeconds(86400));
        assertThat(token.toString()).doesNotContain("secret-token");
        assertThat(http.body.toString()).contains("grant_type=client_credentials");
    }

    private static final class RecordingClient implements KisHttpClient {
        private final KisHttpResponse response;
        private java.net.URI uri;
        private Object body;
        private RecordingClient(KisHttpResponse response){
            this.response=response;
        }
        @Override public KisHttpResponse postJson(java.net.URI uri,
                Map<String,String> headers,Object body){
            this.uri=uri;this.body=body;return response;
        }
        @Override public KisHttpResponse get(java.net.URI uri,
                Map<String,String> headers){throw new UnsupportedOperationException();}
    }
}
