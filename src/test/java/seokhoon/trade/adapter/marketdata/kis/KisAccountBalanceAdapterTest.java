package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.domain.kis.KisEnvironment;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KisAccountBalanceAdapterTest {
    @Test
    void loadsPositiveHoldingsFromSelectedDemoAccount() throws Exception {
        KisHttpClient client = mock(KisHttpClient.class);
        KisAccessTokenProvider tokens = mock(KisAccessTokenProvider.class);
        KisProperties kis = mock(KisProperties.class);
        TradingAccountManagementUseCase accounts = mock(TradingAccountManagementUseCase.class);
        when(accounts.credentials(7L)).thenReturn(Optional.of(
                new TradingAccountManagementUseCase.AccountCredentials(7L, "모의",
                        KisEnvironment.DEMO, "12345678", "01")));
        when(tokens.getAccessToken(KisEnvironment.DEMO)).thenReturn("token");
        when(kis.appKey(KisEnvironment.DEMO)).thenReturn("key");
        when(kis.appSecret(KisEnvironment.DEMO)).thenReturn("secret");
        when(kis.baseUrl(KisEnvironment.DEMO)).thenReturn("https://demo.example");
        var body = new ObjectMapper().readTree("""
                {"rt_cd":"0","output1":[
                  {"pdno":"005930","prdt_name":"삼성전자","hldg_qty":"3",
                   "pchs_avg_pric":"70000","pchs_amt":"210000","prpr":"72000",
                   "evlu_amt":"216000","evlu_pfls_amt":"6000","evlu_pfls_rt":"2.85"},
                  {"pdno":"000660","prdt_name":"전량매도","hldg_qty":"0"}
                ]}
                """);
        when(client.get(any(URI.class), anyMap())).thenReturn(new KisHttpResponse(200, body));

        var rows = new KisAccountBalanceAdapter(client, tokens, kis, accounts).holdings(7L);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.environment()).isEqualTo(KisEnvironment.DEMO);
            assertThat(row.stockCode()).isEqualTo("005930");
            assertThat(row.quantity()).isEqualTo(3);
            assertThat(row.marketValue()).isEqualByComparingTo("216000");
        });
        verify(client).get(argThat(uri -> uri.toString().contains("CANO=12345678")
                && uri.toString().contains("INQR_DVSN=02")), anyMap());
    }
}
