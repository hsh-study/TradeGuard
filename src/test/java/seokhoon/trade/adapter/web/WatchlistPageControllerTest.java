package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class WatchlistPageControllerTest {
    @Test
    void rendersReadOnlyChartAndWatchlistManagement() throws Exception {
        standaloneSetup(new WatchlistPageController()).build()
                .perform(get("/operations/watchlist"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("관심종목 관리 · 차트")))
                .andExpect(content().string(containsString("/api/stocks/chart")))
                .andExpect(content().string(containsString("캔들")))
                .andExpect(content().string(containsString("최근 30일 재료 수집")))
                .andExpect(content().string(containsString("/portfolio/watchlist")))
                .andExpect(content().string(not(containsString("<h2>보유 종목</h2>"))))
                .andExpect(content().string(not(containsString("<h2>지정가 매수·매도</h2>"))))
                .andExpect(content().string(containsString("장초반")))
                .andExpect(content().string(containsString("종베")))
                .andExpect(content().string(containsString("사용자")))
                .andExpect(content().string(containsString("sourceTags(s.tags)")))
                .andExpect(content().string(containsString("실시간 연결 대기")))
                .andExpect(content().string(containsString("EventSource")))
                .andExpect(content().string(containsString("/chart/stream")))
                .andExpect(content().string(containsString("method:'DELETE'")))
                .andExpect(content().string(containsString("overflow-y:auto")))
                .andExpect(content().string(containsString("등록 종목 스크롤 목록")))
                .andExpect(content().string(containsString("MINUTE_60")))
                .andExpect(content().string(containsString("월봉")))
                .andExpect(content().string(containsString("주가수익비율")))
                .andExpect(content().string(containsString("/operations/trading?stockCode=")))
                .andExpect(content().string(containsString("/operations/chart?stockCode=")))
                .andExpect(content().string(not(containsString("realTradingConfirmed"))))
                .andExpect(content().string(containsString("/materials/collect")))
                .andExpect(content().string(not(containsString("appSecret"))))
                .andExpect(content().string(not(containsString("accountNo"))))
                .andExpect(content().string(not(containsString("sourceUrl"))))
                .andExpect(content().string(not(containsString("receiptNo"))))
                .andExpect(content().string(not(containsString("/api/live-orders"))));
    }

    @Test
    void exposesSeparatePortfolioAndTradingPages() throws Exception {
        var mvc = standaloneSetup(new WatchlistPageController()).build();
        mvc.perform(get("/operations/portfolio")).andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>보유종목</h1>")))
                .andExpect(content().string(containsString("주가순자산비율")))
                .andExpect(content().string(containsString("/portfolio/holdings?accountId=")));
        mvc.perform(get("/operations/trading")).andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>지정가 매매</h1>")))
                .andExpect(content().string(containsString("/api/stocks/orderbook")))
                .andExpect(content().string(containsString("/orderbook/stream")))
                .andExpect(content().string(containsString("KIS WebSocket")))
                .andExpect(content().string(not(containsString("setInterval"))))
                .andExpect(content().string(containsString("/api/operations/orders/")));
        mvc.perform(get("/operations/chart")).andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>TradeGuard 실시간 차트</title>")))
                .andExpect(content().string(containsString("EventSource")));
    }
}
