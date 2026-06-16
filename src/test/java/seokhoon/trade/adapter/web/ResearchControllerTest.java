package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.AnalyzeEarningsUseCase;
import seokhoon.trade.application.port.in.ResearchUseCases;
import seokhoon.trade.domain.market.Sector;
import seokhoon.trade.domain.market.SectorDailySnapshot;
import seokhoon.trade.domain.market.SectorType;
import seokhoon.trade.domain.market.StockSectorMapping;
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
                new StubMorningNoteUseCase(),
                new StubSectorUseCase(),
                new StubEarningsDataUseCase(),
                new StubAnalyzeEarningsUseCase(),
                new StubEarningsAnalysisQueryUseCase()
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

        mvc.perform(post("/api/research/sectors")
                        .contentType("application/json")
                        .content("""
                                {"sectorCode":"SEMICONDUCTOR","sectorName":"반도체","sectorType":"THEME"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectorCode").value("SEMICONDUCTOR"));
        mvc.perform(post("/api/research/sectors/SEMICONDUCTOR/stocks")
                        .contentType("application/json")
                        .content("""
                                {"stockCode":"005930","source":"MANUAL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockCode").value("005930"));
        mvc.perform(get("/api/research/sectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sectorName").value("반도체"));
        mvc.perform(get("/api/research/sectors/SEMICONDUCTOR/snapshot")
                        .param("tradeDate", "2026-06-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadingStockCode").value("005930"));
        mvc.perform(post("/api/research/sectors/snapshots")
                        .param("tradeDate", "2026-06-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedCount").value(1));

        mvc.perform(post("/api/research/financials/quarterly")
                        .contentType("application/json")
                        .content("""
                                [{
                                  "stockCode":"005930",
                                  "fiscalYear":2026,
                                  "fiscalQuarter":1,
                                  "revenue":1000,
                                  "operatingIncome":150,
                                  "netIncome":100,
                                  "totalAssets":5000,
                                  "totalLiabilities":2000,
                                  "totalEquity":3000,
                                  "operatingCashFlow":120,
                                  "freeCashFlow":80
                                }]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockCode").value("005930"));
        mvc.perform(post("/api/research/valuations")
                        .contentType("application/json")
                        .content("""
                                {
                                  "stockCode":"005930",
                                  "tradeDate":"2026-06-15",
                                  "marketCap":500000000000000,
                                  "per":12,
                                  "pbr":1.2,
                                  "psr":1.8
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.per").value(12));
        mvc.perform(post("/api/research/earnings-analysis")
                        .param("stockCode", "005930")
                        .param("baseDate", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STRONG"));
        mvc.perform(post("/api/research/earnings-analysis/batch")
                        .param("baseDate", "2026-06-15")
                        .contentType("application/json")
                        .content("""
                                {"stockCodes":["005930"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockCode").value("005930"));
        mvc.perform(get("/api/research/earnings-analysis")
                        .param("stockCode", "005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(65));
        mvc.perform(get("/api/research/earnings-analysis")
                        .param("baseDate", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("STRONG"));
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

    private static class StubSectorUseCase implements ResearchUseCases.SectorUseCase {
        @Override
        public Sector create(ResearchUseCases.CreateSectorCommand command) {
            return sector();
        }

        @Override
        public StockSectorMapping addStock(String sectorCode, ResearchUseCases.AddSectorStockCommand command) {
            return new StockSectorMapping(1L, command.stockCode(), sectorCode, command.source(), NOW, NOW);
        }

        @Override
        public List<Sector> findAll() {
            return List.of(sector());
        }

        @Override
        public SectorDailySnapshot loadSnapshot(String sectorCode, LocalDate tradeDate) {
            return snapshot();
        }

        @Override
        public ResearchUseCases.SectorSnapshotGenerationResult generateSnapshots(LocalDate tradeDate) {
            return new ResearchUseCases.SectorSnapshotGenerationResult(tradeDate, 1, 1, 0);
        }
    }

    private static class StubEarningsDataUseCase implements ResearchUseCases.EarningsDataUseCase {
        @Override
        public List<QuarterlyFinancial> saveQuarterly(
                List<ResearchUseCases.CreateQuarterlyFinancialCommand> commands
        ) {
            return List.of(financial());
        }

        @Override
        public ValuationSnapshot saveValuation(ResearchUseCases.CreateValuationSnapshotCommand command) {
            return valuation();
        }
    }

    private static class StubAnalyzeEarningsUseCase implements AnalyzeEarningsUseCase {
        @Override
        public EarningsAnalysisSnapshot analyzeStock(String stockCode, LocalDate baseDate) {
            return earnings();
        }

        @Override
        public List<EarningsAnalysisSnapshot> analyzeStocks(List<String> stockCodes, LocalDate baseDate) {
            return List.of(earnings());
        }
    }

    private static class StubEarningsAnalysisQueryUseCase implements ResearchUseCases.EarningsAnalysisQueryUseCase {
        @Override
        public EarningsAnalysisSnapshot findLatestByStockCode(String stockCode) {
            return earnings();
        }

        @Override
        public List<EarningsAnalysisSnapshot> findByBaseDate(LocalDate baseDate) {
            return List.of(earnings());
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

    private static Sector sector() {
        return new Sector(1L, "SEMICONDUCTOR", "반도체", SectorType.THEME, NOW, NOW);
    }

    private static SectorDailySnapshot snapshot() {
        return new SectorDailySnapshot(1L, "SEMICONDUCTOR", LocalDate.of(2026, 6, 12),
                new BigDecimal("2.5000"), new BigDecimal("2.5000"),
                new BigDecimal("100000000"), 1, 0, "005930",
                new BigDecimal("2.5000"), NOW, NOW);
    }

    private static QuarterlyFinancial financial() {
        return new QuarterlyFinancial(1L, "005930", 2026, 1,
                new BigDecimal("1000"), new BigDecimal("150"), new BigDecimal("100"),
                new BigDecimal("5000"), new BigDecimal("2000"), new BigDecimal("3000"),
                new BigDecimal("120"), new BigDecimal("80"), NOW, NOW);
    }

    private static ValuationSnapshot valuation() {
        return new ValuationSnapshot(1L, "005930", LocalDate.of(2026, 6, 15),
                new BigDecimal("500000000000000"), new BigDecimal("12"),
                new BigDecimal("1.2"), new BigDecimal("1.8"),
                null, null, null, NOW, NOW);
    }

    private static EarningsAnalysisSnapshot earnings() {
        return new EarningsAnalysisSnapshot(1L, "005930", LocalDate.of(2026, 6, 15),
                new BigDecimal("0.1230"), new BigDecimal("0.2000"), new BigDecimal("0.1000"),
                new BigDecimal("0.1500"), new BigDecimal("0.1000"), new BigDecimal("0.6667"),
                new BigDecimal("120"), new BigDecimal("80"),
                new BigDecimal("12"), new BigDecimal("1.2"), new BigDecimal("1.8"),
                30, 35, 65, EarningsAnalysisStatus.STRONG,
                List.of("REVENUE_YOY_OVER_10PCT"), NOW, NOW);
    }
}
