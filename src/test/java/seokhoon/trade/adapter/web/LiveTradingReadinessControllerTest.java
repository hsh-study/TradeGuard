package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.LiveTradingReadinessUseCase;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.order.LiveTradingReadinessReport;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LiveTradingReadinessControllerTest {
    @Test
    void exposesReadinessWithoutSensitiveValues() throws Exception {
        LiveTradingReadinessUseCase useCase=mock(
                LiveTradingReadinessUseCase.class);
        when(useCase.checkReadiness()).thenReturn(report());
        MockMvc mvc=MockMvcBuilders.standaloneSetup(
                new LiveTradingReadinessController(useCase)).build();

        String body=mvc.perform(get("/api/live-trading/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.tradingEnvironment").value("REAL"))
                .andExpect(jsonPath("$.accountConfigured").value(true))
                .andExpect(jsonPath("$.tokenStatus.status").value("VALID"))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("raw-token")
                .doesNotContain("account-number")
                .doesNotContain("app-secret")
                .doesNotContain("app-key")
                .doesNotContain("accessToken");
    }

    private static LiveTradingReadinessReport report() {
        return new LiveTradingReadinessReport(true,true,
                KisEnvironment.REAL,KisEnvironment.DEMO,true,
                new LiveTradingReadinessReport.TokenStatus(true,
                        Instant.parse("2026-06-15T10:00:00Z"),
                        3600,"VALID"),
                false,
                new LiveTradingReadinessReport.MarketCalendarStatus(
                        true,true,"DB"),
                true,"LIMIT",new BigDecimal("1000000"),true,
                new LiveTradingReadinessReport.AutoCancelPolicy(
                        false,3,3,5),
                List.of("LIVE_ORDER_AUTO_CANCEL_DISABLED"),
                List.of(),true);
    }
}
