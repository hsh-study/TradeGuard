package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReplayBacktestControllerTest {
    @Test
    void exposesClosingAndEarlyMarketReplayEndpoints() throws Exception {
        RecordingUseCase useCase = new RecordingUseCase();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ReplayBacktestController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/api/research/backtests/replay/closing-bet")
                        .param("from", "2026-06-01").param("to", "2026-06-15").param("holdingDays", "2"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.run.id").value(1));
        assertThat(useCase.holdingDays).isEqualTo(2);

        mvc.perform(post("/api/research/backtests/replay/early-market")
                        .param("from", "2026-06-01").param("to", "2026-06-15")
                        .param("entryTime", "09:05").param("exitTime", "09:31"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.run.strategy").value("EARLY_MARKET"));
        assertThat(useCase.entryTime).isEqualTo(LocalTime.of(9, 5));

        mvc.perform(get("/api/research/backtests/replay/runs/1/results"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].stockCode").value("005930"));
    }

    private static class RecordingUseCase implements RunReplayBacktestUseCase {
        int holdingDays; LocalTime entryTime;
        public ReplayBacktestRunView runClosingBet(LocalDate f, LocalDate t, int h) { holdingDays = h; return view(ReplayBacktestStrategy.CLOSING_BET); }
        public ReplayBacktestRunView runEarlyMarket(LocalDate f, LocalDate t, LocalTime e, LocalTime x) { entryTime = e; return view(ReplayBacktestStrategy.EARLY_MARKET); }
        public ReplayBacktestRunView getRun(long id) { return view(ReplayBacktestStrategy.CLOSING_BET); }
        public List<ReplayBacktestResult> getResults(long id) {
            return List.of(new ReplayBacktestResult(1L, 1L, LocalDate.of(2026,6,1), "005930", "삼성전자",
                    ReplayBacktestStrategy.CLOSING_BET, 1, 80, List.of(), List.of(), BigDecimal.TEN,
                    BigDecimal.TEN, 1, BigDecimal.ZERO, ReplayBacktestResultStatus.FLAT, Instant.EPOCH));
        }
        private static ReplayBacktestRunView view(ReplayBacktestStrategy strategy) {
            ReplayBacktestRun run = new ReplayBacktestRun(1L, strategy, LocalDate.of(2026,6,1), LocalDate.of(2026,6,15),
                    ReplayBacktestStatus.COMPLETED, "{}", 0, 0, 0, null, null, null, null, Instant.EPOCH, Instant.EPOCH);
            return new ReplayBacktestRunView(run, null, null, null, null, List.of(), List.of());
        }
    }
}
