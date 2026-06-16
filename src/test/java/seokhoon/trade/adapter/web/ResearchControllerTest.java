package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.ResearchUseCases;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResearchControllerTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Test
    void exposesThesisCatalystAndMorningNoteApis() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ResearchController(
                new StubThesisUseCase(),
                new StubCatalystUseCase(),
                new StubMorningNoteUseCase()
        )).setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/api/research/theses")
                        .contentType("application/json")
                        .content("""
                                {
                                  "stockCode":"005930",
                                  "title":"HBM recovery",
                                  "coreAssumption":"memory margin improves",
                                  "invalidationCondition":"margin declines",
                                  "targetPrice":90000,
                                  "stopLossCondition":"close below MA60",
                                  "confidence":75
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockCode").value("005930"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(get("/api/research/theses")
                        .param("stockCode", "005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("HBM recovery"));
        mvc.perform(patch("/api/research/theses/1")
                        .contentType("application/json")
                        .content("""
                                {"status":"BROKEN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BROKEN"));
        mvc.perform(post("/api/research/theses/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mvc.perform(post("/api/research/catalysts")
                        .contentType("application/json")
                        .content("""
                                {
                                  "stockCode":"005930",
                                  "title":"2Q earnings",
                                  "catalystType":"EARNINGS",
                                  "expectedDate":"2026-07-31",
                                  "importance":"HIGH"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPCOMING"));
        mvc.perform(get("/api/research/catalysts")
                        .param("from", "2026-07-01")
                        .param("to", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("2Q earnings"));
        mvc.perform(patch("/api/research/catalysts/1")
                        .contentType("application/json")
                        .content("""
                                {"status":"OCCURRED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OCCURRED"));

        mvc.perform(post("/api/research/morning-note")
                        .param("tradeDate", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionItems").value("자동 주문 없음"));
        mvc.perform(get("/api/research/morning-note")
                        .param("tradeDate", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketSummary").value("market"));
    }

    private static class StubThesisUseCase implements ResearchUseCases.ThesisUseCase {
        @Override
        public InvestmentThesis create(ResearchUseCases.CreateThesisCommand command) {
            return thesis(ThesisStatus.ACTIVE);
        }

        @Override
        public List<InvestmentThesis> find(String stockCode) {
            return List.of(thesis(ThesisStatus.ACTIVE));
        }

        @Override
        public InvestmentThesis update(long id, ResearchUseCases.UpdateThesisCommand command) {
            return thesis(command.status());
        }

        @Override
        public InvestmentThesis close(long id) {
            return thesis(ThesisStatus.CLOSED);
        }
    }

    private static class StubCatalystUseCase implements ResearchUseCases.CatalystUseCase {
        @Override
        public InvestmentCatalyst create(ResearchUseCases.CreateCatalystCommand command) {
            return catalyst(CatalystStatus.UPCOMING);
        }

        @Override
        public List<InvestmentCatalyst> find(String stockCode, LocalDate from, LocalDate to) {
            return List.of(catalyst(CatalystStatus.UPCOMING));
        }

        @Override
        public InvestmentCatalyst update(long id, ResearchUseCases.UpdateCatalystCommand command) {
            return catalyst(command.status());
        }
    }

    private static class StubMorningNoteUseCase implements ResearchUseCases.MorningNoteUseCase {
        @Override
        public MorningNote generate(LocalDate tradeDate) {
            return new MorningNote(1L, tradeDate, "market", "sector",
                    "portfolio", "watchlist", "자동 주문 없음", NOW);
        }

        @Override
        public MorningNote load(LocalDate tradeDate) {
            return generate(tradeDate);
        }
    }

    private static InvestmentThesis thesis(ThesisStatus status) {
        return new InvestmentThesis(1L, "005930", "HBM recovery",
                "memory margin improves", "margin declines",
                new BigDecimal("90000"), "close below MA60",
                75, status, NOW, NOW);
    }

    private static InvestmentCatalyst catalyst(CatalystStatus status) {
        return new InvestmentCatalyst(1L, "005930", "2Q earnings",
                CatalystType.EARNINGS, LocalDate.of(2026, 7, 31),
                CatalystImportance.HIGH, status, null, null, NOW, NOW);
    }
}
