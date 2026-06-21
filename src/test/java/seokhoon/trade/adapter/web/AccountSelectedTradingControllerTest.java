package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.AccountSelectedTradingUseCase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AccountSelectedTradingControllerTest {
    @Test
    void acceptsSelectedAccountAndRealConfirmationFlag() throws Exception {
        AccountSelectedTradingUseCase useCase = mock(AccountSelectedTradingUseCase.class);
        standaloneSetup(new AccountSelectedTradingController(useCase)).build()
                .perform(post("/api/operations/orders/buy")
                        .contentType("application/json")
                        .content("""
                                {"accountId":2,"stockCode":"005930","quantity":1,
                                 "orderPrice":70000,"orderType":"LIMIT",
                                 "realTradingConfirmed":true}
                                """))
                .andExpect(status().isOk());
        verify(useCase).buy(any());
    }
}
