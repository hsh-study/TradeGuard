package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.application.port.out.StockOrderBookPort;
import seokhoon.trade.domain.kis.KisEnvironment;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KisStockOrderBookAdapterTest {
    @Test
    void mapsAskAndBidLevelsForSelectedAccountEnvironment() throws Exception {
        KisHttpClient client = mock(KisHttpClient.class);
        KisAccessTokenProvider tokens = mock(KisAccessTokenProvider.class);
        KisProperties kis = mock(KisProperties.class);
        TradingAccountManagementUseCase accounts = mock(TradingAccountManagementUseCase.class);
        when(accounts.credentials(2L)).thenReturn(Optional.of(
                new TradingAccountManagementUseCase.AccountCredentials(2L, "real",
                        KisEnvironment.REAL, "12345678", "01")));
        when(tokens.getAccessToken(KisEnvironment.REAL)).thenReturn("token");
        when(kis.baseUrl(KisEnvironment.REAL)).thenReturn("https://real.example");
        when(kis.appKey(KisEnvironment.REAL)).thenReturn("key");
        when(kis.appSecret(KisEnvironment.REAL)).thenReturn("secret");
        var body = new ObjectMapper().readTree("""
                {"rt_cd":"0","output1":{"askp1":"70100","askp_rsqn1":"12",
                "bidp1":"69900","bidp_rsqn1":"15"},"output2":{"antc_cnpr":"70000"}}
                """);
        when(client.get(any(URI.class), anyMap())).thenReturn(new KisHttpResponse(200, body));

        var result = new KisStockOrderBookAdapter(client, tokens, kis, accounts)
                .load("005930", 2L);

        assertThat(result.currentPrice()).isEqualByComparingTo("70000");
        assertThat(result.asks()).singleElement().satisfies(level -> {
            assertThat(level.price()).isEqualByComparingTo("70100");
            assertThat(level.quantity()).isEqualTo(12);
        });
        assertThat(result.bids()).singleElement().extracting(StockOrderBookPort.Level::quantity)
                .isEqualTo(15L);
    }
}
