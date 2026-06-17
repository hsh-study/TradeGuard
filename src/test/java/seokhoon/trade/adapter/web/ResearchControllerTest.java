package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.AnalyzeEarningsUseCase;
import seokhoon.trade.application.port.in.GenerateEarningsPreviewUseCase;
import seokhoon.trade.application.port.in.ImportDartFinancialsUseCase;
import seokhoon.trade.application.port.in.ResearchUseCases;
import seokhoon.trade.domain.stock.Market;
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
                new StubEarningsAnalysisQueryUseCase(),
                new StubEarningsEventUseCase(),
                new StubEarningsPreviewUseCase(),
                new StubGenerateEarningsPreviewUseCase(),
                new StubPostEarningsReviewUseCase(),
                new StubDartCorpMappingUseCase(),
                new StubImportDartFinancialsUseCase(),
                new StubDartFinancialImportHistoryQueryUseCase()
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

        mvc.perform(post("/api/research/earnings-events")
                        .contentType("application/json")
                        .content("""
                                {
                                  "stockCode":"005930",
                                  "fiscalYear":2026,
                                  "fiscalQuarter":2,
                                  "expectedAnnouncementDate":"2026-07-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
        mvc.perform(get("/api/research/earnings-events")
                        .param("from", "2026-07-01")
                        .param("to", "2026-08-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockCode").value("005930"));
        mvc.perform(get("/api/research/earnings-events")
                        .param("stockCode", "005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fiscalQuarter").value(2));
        mvc.perform(patch("/api/research/earnings-events/1")
                        .contentType("application/json")
                        .content("""
                                {"status":"ANNOUNCED","actualAnnouncementDate":"2026-07-31"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANNOUNCED"));
        mvc.perform(post("/api/research/earnings-previews")
                        .contentType("application/json")
                        .content("""
                                {
                                  "earningsEventId":1,
                                  "stockCode":"005930",
                                  "previewDate":"2026-07-25",
                                  "keyCheckpoints":["HBM margin"],
                                  "expectedRevenue":1000,
                                  "expectedOperatingIncome":150,
                                  "expectedNetIncome":100,
                                  "status":"READY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
        mvc.perform(post("/api/research/earnings-previews/generate")
                        .param("stockCode", "005930")
                        .param("earningsEventId", "1")
                        .param("previewDate", "2026-07-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyCheckpoints[0]").value("HBM margin"));
        mvc.perform(get("/api/research/earnings-previews")
                        .param("stockCode", "005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("READY"));
        mvc.perform(get("/api/research/earnings-previews/upcoming")
                        .param("from", "2026-07-20")
                        .param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockCode").value("005930"));
        mvc.perform(post("/api/research/post-earnings-reviews")
                        .contentType("application/json")
                        .content("""
                                {
                                  "earningsEventId":1,
                                  "stockCode":"005930",
                                  "reviewDate":"2026-07-31",
                                  "actualRevenue":1100,
                                  "actualOperatingIncome":180,
                                  "actualNetIncome":120,
                                  "thesisImpact":"STRENGTHENED",
                                  "reviewSummary":"Beat expectations",
                                  "actionItems":[],
                                  "upsertQuarterlyFinancial":false,
                                  "rerunEarningsAnalysis":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thesisImpact").value("STRENGTHENED"));
        mvc.perform(get("/api/research/post-earnings-reviews")
                        .param("stockCode", "005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewSummary").value("Beat expectations"));

        mvc.perform(post("/api/research/dart/corp-mappings")
                        .contentType("application/json")
                        .content("""
                                {
                                  "stockCode":"005930",
                                  "corpCode":"00126380",
                                  "corpName":"삼성전자",
                                  "market":"KOSPI"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corpCode").value("00126380"));
        mvc.perform(get("/api/research/dart/corp-mappings")
                        .param("stockCode", "005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corpName").value("삼성전자"));
        mvc.perform(get("/api/research/dart/corp-mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockCode").value("005930"));
        mvc.perform(post("/api/research/dart/financials/import")
                        .param("stockCode", "005930")
                        .param("fiscalYear", "2026")
                        .param("reportCode", "11013"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
        mvc.perform(post("/api/research/dart/financials/import-recent")
                        .param("stockCode", "005930")
                        .param("baseDate", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportCode").value("11013"));
        mvc.perform(post("/api/research/dart/financials/import-watchlist")
                        .param("baseDate", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockCode").value("005930"));
        mvc.perform(get("/api/research/dart/financials/import-histories")
                        .param("stockCode", "005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
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

    private static class StubEarningsEventUseCase implements ResearchUseCases.EarningsEventUseCase {
        @Override
        public EarningsEvent create(ResearchUseCases.CreateEarningsEventCommand command) {
            return event(command.status() == null ? EarningsEventStatus.SCHEDULED : command.status());
        }

        @Override
        public List<EarningsEvent> find(String stockCode, LocalDate from, LocalDate to) {
            return List.of(event(EarningsEventStatus.SCHEDULED));
        }

        @Override
        public EarningsEvent update(long id, ResearchUseCases.UpdateEarningsEventCommand command) {
            return event(command.status());
        }
    }

    private static class StubEarningsPreviewUseCase implements ResearchUseCases.EarningsPreviewUseCase {
        @Override
        public EarningsPreview create(ResearchUseCases.CreateEarningsPreviewCommand command) {
            return preview();
        }

        @Override
        public List<EarningsPreview> findByStockCode(String stockCode) {
            return List.of(preview());
        }

        @Override
        public List<EarningsPreview> findUpcomingReady(LocalDate from, LocalDate to) {
            return List.of(preview());
        }
    }

    private static class StubGenerateEarningsPreviewUseCase implements GenerateEarningsPreviewUseCase {
        @Override
        public EarningsPreview generate(String stockCode, long earningsEventId, LocalDate previewDate) {
            return preview();
        }
    }

    private static class StubPostEarningsReviewUseCase implements ResearchUseCases.PostEarningsReviewUseCase {
        @Override
        public PostEarningsReview create(ResearchUseCases.CreatePostEarningsReviewCommand command) {
            return review(command.thesisImpact(), command.reviewSummary());
        }

        @Override
        public List<PostEarningsReview> findByStockCode(String stockCode) {
            return List.of(review(ThesisImpact.STRENGTHENED, "Beat expectations"));
        }
    }

    private static class StubDartCorpMappingUseCase implements ResearchUseCases.DartCorpMappingUseCase {
        @Override
        public DartCorpMapping save(ResearchUseCases.SaveDartCorpMappingCommand command) {
            return dartMapping();
        }

        @Override
        public java.util.Optional<DartCorpMapping> findByStockCode(String stockCode) {
            return java.util.Optional.of(dartMapping());
        }

        @Override
        public List<DartCorpMapping> findAll() {
            return List.of(dartMapping());
        }
    }

    private static class StubImportDartFinancialsUseCase implements ImportDartFinancialsUseCase {
        @Override
        public DartFinancialImportHistory importStock(String stockCode, int fiscalYear, String reportCode) {
            return dartHistory(reportCode);
        }

        @Override
        public List<DartFinancialImportHistory> importStockRecent(String stockCode, LocalDate baseDate) {
            return List.of(dartHistory("11013"));
        }

        @Override
        public List<DartFinancialImportHistory> importActiveWatchlist(LocalDate baseDate) {
            return List.of(dartHistory("11013"));
        }
    }

    private static class StubDartFinancialImportHistoryQueryUseCase
            implements ResearchUseCases.DartFinancialImportHistoryQueryUseCase {
        @Override
        public List<DartFinancialImportHistory> findByStockCode(String stockCode) {
            return List.of(dartHistory("11013"));
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

    private static EarningsEvent event(EarningsEventStatus status) {
        return new EarningsEvent(1L, "005930", 2026, 2,
                LocalDate.of(2026, 7, 31),
                status == EarningsEventStatus.ANNOUNCED ? LocalDate.of(2026, 7, 31) : null,
                status, null, NOW, NOW);
    }

    private static EarningsPreview preview() {
        return new EarningsPreview(1L, 1L, "005930",
                LocalDate.of(2026, 7, 25), List.of("HBM margin"),
                new BigDecimal("1000"), new BigDecimal("150"), new BigDecimal("100"),
                new BigDecimal("0.1500"), List.of("FX risk"),
                List.of("margin improves"), EarningsPreviewStatus.READY, NOW, NOW);
    }

    private static PostEarningsReview review(ThesisImpact impact, String summary) {
        return new PostEarningsReview(1L, 1L, "005930",
                LocalDate.of(2026, 7, 31), new BigDecimal("1100"),
                new BigDecimal("180"), new BigDecimal("120"),
                new BigDecimal("0.1636"), new BigDecimal("0.1000"),
                new BigDecimal("0.2000"), impact, summary, List.of(), NOW, NOW);
    }

    private static DartCorpMapping dartMapping() {
        return new DartCorpMapping(1L, "005930", "00126380", "삼성전자",
                Market.KOSPI, NOW, NOW);
    }

    private static DartFinancialImportHistory dartHistory(String reportCode) {
        return new DartFinancialImportHistory(1L, "005930", "00126380", 2026,
                reportCode, DartFinancialImportStatus.SUCCESS, 1, null, NOW, NOW);
    }
}
