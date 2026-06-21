package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TradingAccountPageControllerTest {
    @Test void rendersAccountManagementWithoutStoredSecrets() throws Exception {
        MockMvcBuilders.standaloneSetup(new TradingAccountPageController()).build()
                .perform(get("/operations/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("거래 계좌 관리")))
                .andExpect(content().string(containsString("/api/trading-accounts")))
                .andExpect(content().string(not(containsString("KIS_APP_SECRET"))));
    }
}
