package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.domain.research.*;

import java.time.*;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaperTradingReportControllerTest {
    @Test void exposesDailyLatestRunAndResults() throws Exception {
        GeneratePaperTradingReportUseCase useCase=new GeneratePaperTradingReportUseCase() {
            public PaperTradingReportView generateDailyReport(LocalDate d){return view();} public PaperTradingReportView getRun(long id){return view();}
            public List<PaperTradingReportResult> getResults(long id){return List.of();} public PaperTradingReportView getLatestByTradeDate(LocalDate d){return view();}
        };
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new PaperTradingReportController(useCase)).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/research/paper-trading/reports/daily").param("tradeDate","2026-06-15")).andExpect(status().isCreated()).andExpect(jsonPath("$.run.id").value(1));
        mvc.perform(get("/api/research/paper-trading/reports/latest").param("tradeDate","2026-06-15")).andExpect(status().isOk());
        mvc.perform(get("/api/research/paper-trading/reports/runs/1/results")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }
    private static PaperTradingReportView view() {
        return new PaperTradingReportView(new PaperTradingReportRun(1L,LocalDate.of(2026,6,15),PaperTradingReportStatus.COMPLETED,
                0,null,0,0,0,null,Instant.EPOCH,Instant.EPOCH),null,0,List.of(),List.of(),List.of(),List.of(),List.of());
    }
}
