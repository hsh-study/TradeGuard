package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
import seokhoon.trade.domain.kis.KisEnvironment;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TradingAccountControllerTest {
    @Test void listsOnlyMaskedAccountNumbers() throws Exception {
        TradingAccountManagementUseCase useCase = mock(TradingAccountManagementUseCase.class);
        when(useCase.list()).thenReturn(List.of(new TradingAccountManagementUseCase.AccountView(
                1L, "실전", KisEnvironment.REAL, "******78", "01", true, true,
                Instant.parse("2026-06-20T00:00:00Z"), Instant.parse("2026-06-20T00:00:00Z"))));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TradingAccountController(useCase)).build();

        mvc.perform(get("/api/trading-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].maskedAccountNumber").value("******78"))
                .andExpect(content().string(not(containsString("12345678"))));
    }
}
