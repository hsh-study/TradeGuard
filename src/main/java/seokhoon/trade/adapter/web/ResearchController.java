package seokhoon.trade.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.AnalyzeEarningsUseCase;
import seokhoon.trade.application.port.in.DartCorpCodeImportUseCase;
import seokhoon.trade.application.port.in.GenerateEarningsPreviewUseCase;
import seokhoon.trade.application.port.in.GenerateValuationSnapshotUseCase;
import seokhoon.trade.application.port.in.ImportDartFinancialsUseCase;
import seokhoon.trade.application.port.in.ImportSharesOutstandingUseCase;
import seokhoon.trade.application.port.in.ResearchUseCases.*;
import seokhoon.trade.application.service.ResearchNotFoundException;
import seokhoon.trade.domain.market.Sector;
import seokhoon.trade.domain.market.SectorDailySnapshot;
import seokhoon.trade.domain.market.SectorType;
import seokhoon.trade.domain.market.StockSectorMapping;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/research")
public class ResearchController {
    private final ThesisUseCase thesisUseCase;
    private final CatalystUseCase catalystUseCase;
    private final MorningNoteUseCase morningNoteUseCase;
    private final SectorUseCase sectorUseCase;
    private final EarningsDataUseCase earningsDataUseCase;
    private final GenerateValuationSnapshotUseCase generateValuationSnapshotUseCase;
    private final AnalyzeEarningsUseCase analyzeEarningsUseCase;
    private final EarningsAnalysisQueryUseCase earningsAnalysisQueryUseCase;
    private final EarningsEventUseCase earningsEventUseCase;
    private final EarningsPreviewUseCase earningsPreviewUseCase;
    private final GenerateEarningsPreviewUseCase generateEarningsPreviewUseCase;
    private final PostEarningsReviewUseCase postEarningsReviewUseCase;
    private final DartCorpMappingUseCase dartCorpMappingUseCase;
    private final DartCorpCodeImportUseCase dartCorpCodeImportUseCase;
    private final ImportDartFinancialsUseCase importDartFinancialsUseCase;
    private final DartFinancialImportHistoryQueryUseCase dartHistoryQueryUseCase;
    private final ImportSharesOutstandingUseCase importSharesOutstandingUseCase;

    public ResearchController(
            ThesisUseCase thesisUseCase,
            CatalystUseCase catalystUseCase,
            MorningNoteUseCase morningNoteUseCase,
            SectorUseCase sectorUseCase,
            EarningsDataUseCase earningsDataUseCase,
            GenerateValuationSnapshotUseCase generateValuationSnapshotUseCase,
            AnalyzeEarningsUseCase analyzeEarningsUseCase,
            EarningsAnalysisQueryUseCase earningsAnalysisQueryUseCase,
            EarningsEventUseCase earningsEventUseCase,
            EarningsPreviewUseCase earningsPreviewUseCase,
            GenerateEarningsPreviewUseCase generateEarningsPreviewUseCase,
            PostEarningsReviewUseCase postEarningsReviewUseCase,
            DartCorpMappingUseCase dartCorpMappingUseCase,
            DartCorpCodeImportUseCase dartCorpCodeImportUseCase,
            ImportDartFinancialsUseCase importDartFinancialsUseCase,
            DartFinancialImportHistoryQueryUseCase dartHistoryQueryUseCase,
            ImportSharesOutstandingUseCase importSharesOutstandingUseCase
    ) {
        this.thesisUseCase = thesisUseCase;
        this.catalystUseCase = catalystUseCase;
        this.morningNoteUseCase = morningNoteUseCase;
        this.sectorUseCase = sectorUseCase;
        this.earningsDataUseCase = earningsDataUseCase;
        this.generateValuationSnapshotUseCase = generateValuationSnapshotUseCase;
        this.analyzeEarningsUseCase = analyzeEarningsUseCase;
        this.earningsAnalysisQueryUseCase = earningsAnalysisQueryUseCase;
        this.earningsEventUseCase = earningsEventUseCase;
        this.earningsPreviewUseCase = earningsPreviewUseCase;
        this.generateEarningsPreviewUseCase = generateEarningsPreviewUseCase;
        this.postEarningsReviewUseCase = postEarningsReviewUseCase;
        this.dartCorpMappingUseCase = dartCorpMappingUseCase;
        this.dartCorpCodeImportUseCase = dartCorpCodeImportUseCase;
        this.importDartFinancialsUseCase = importDartFinancialsUseCase;
        this.dartHistoryQueryUseCase = dartHistoryQueryUseCase;
        this.importSharesOutstandingUseCase = importSharesOutstandingUseCase;
    }

    @PostMapping("/theses")
    InvestmentThesis createThesis(@Valid @RequestBody ThesisRequest request) {
        return thesisUseCase.create(request.toCreateCommand());
    }

    @GetMapping("/theses")
    List<InvestmentThesis> findTheses(@RequestParam(required = false) String stockCode) {
        return thesisUseCase.find(stockCode);
    }

    @PatchMapping("/theses/{id}")
    InvestmentThesis updateThesis(
            @PathVariable long id,
            @Valid @RequestBody ThesisPatchRequest request
    ) {
        return thesisUseCase.update(id, request.toCommand());
    }

    @PostMapping("/theses/{id}/close")
    InvestmentThesis closeThesis(@PathVariable long id) {
        return thesisUseCase.close(id);
    }

    @PostMapping("/catalysts")
    InvestmentCatalyst createCatalyst(@Valid @RequestBody CatalystRequest request) {
        return catalystUseCase.create(request.toCreateCommand());
    }

    @GetMapping("/catalysts")
    List<InvestmentCatalyst> findCatalysts(
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return catalystUseCase.find(stockCode, from, to);
    }

    @PatchMapping("/catalysts/{id}")
    InvestmentCatalyst updateCatalyst(
            @PathVariable long id,
            @RequestBody CatalystPatchRequest request
    ) {
        return catalystUseCase.update(id, request.toCommand());
    }

    @PostMapping("/morning-note")
    MorningNote generateMorningNote(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return morningNoteUseCase.generate(tradeDate);
    }

    @GetMapping("/morning-note")
    MorningNote loadMorningNote(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return morningNoteUseCase.load(tradeDate);
    }

    @PostMapping("/sectors")
    Sector createSector(@Valid @RequestBody SectorRequest request) {
        return sectorUseCase.create(request.toCommand());
    }

    @PostMapping("/sectors/{sectorCode}/stocks")
    StockSectorMapping addSectorStock(
            @PathVariable String sectorCode,
            @Valid @RequestBody SectorStockRequest request
    ) {
        return sectorUseCase.addStock(sectorCode, request.toCommand());
    }

    @GetMapping("/sectors")
    List<Sector> findSectors() {
        return sectorUseCase.findAll();
    }

    @GetMapping("/sectors/{sectorCode}/snapshot")
    SectorDailySnapshot loadSectorSnapshot(
            @PathVariable String sectorCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return sectorUseCase.loadSnapshot(sectorCode, tradeDate);
    }

    @PostMapping("/sectors/snapshots")
    SectorSnapshotGenerationResult generateSectorSnapshots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return sectorUseCase.generateSnapshots(tradeDate);
    }

    @PostMapping("/financials/quarterly")
    List<QuarterlyFinancial> saveQuarterlyFinancials(@Valid @RequestBody List<QuarterlyFinancialRequest> requests) {
        return earningsDataUseCase.saveQuarterly(requests.stream()
                .map(QuarterlyFinancialRequest::toCommand)
                .toList());
    }

    @PostMapping("/valuations")
    ValuationSnapshot saveValuation(@Valid @RequestBody ValuationSnapshotRequest request) {
        return earningsDataUseCase.saveValuation(request.toCommand());
    }

    @PostMapping("/valuations/shares-outstanding")
    SharesOutstandingSnapshot saveSharesOutstanding(@Valid @RequestBody SharesOutstandingRequest request) {
        return earningsDataUseCase.saveSharesOutstanding(request.toCommand());
    }

    @GetMapping("/valuations/shares-outstanding")
    List<SharesOutstandingSnapshot> findSharesOutstanding(@RequestParam String stockCode) {
        return earningsDataUseCase.findSharesOutstanding(stockCode);
    }

    @PostMapping("/valuations/shares-outstanding/import-csv")
    SharesOutstandingImportHistory importSharesOutstandingCsv(@RequestBody String csv) {
        return importSharesOutstandingUseCase.importCsv(csv);
    }

    @GetMapping("/valuations/shares-outstanding/import-histories")
    List<SharesOutstandingImportHistory> findSharesOutstandingImportHistories() {
        return importSharesOutstandingUseCase.findSharesOutstandingImportHistories();
    }

    @PostMapping("/valuations/generate")
    ValuationGenerationResult generateValuation(
            @RequestParam String stockCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        return generateValuationSnapshotUseCase.generate(stockCode, baseDate);
    }

    @PostMapping("/valuations/generate-batch")
    List<ValuationGenerationResult> generateValuationBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @Valid @RequestBody ValuationGenerationBatchRequest request
    ) {
        return generateValuationSnapshotUseCase.generateBatch(request.stockCodes(), baseDate);
    }

    @PostMapping("/valuations/generate-watchlist")
    List<ValuationGenerationResult> generateValuationWatchlist(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        return generateValuationSnapshotUseCase.generateWatchlist(baseDate);
    }

    @PostMapping("/earnings-analysis")
    EarningsAnalysisSnapshot analyzeEarnings(
            @RequestParam String stockCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        return analyzeEarningsUseCase.analyzeStock(stockCode, baseDate);
    }

    @PostMapping("/earnings-analysis/batch")
    List<EarningsAnalysisSnapshot> analyzeEarningsBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @Valid @RequestBody EarningsAnalysisBatchRequest request
    ) {
        return analyzeEarningsUseCase.analyzeStocks(request.stockCodes(), baseDate);
    }

    @GetMapping(value = "/earnings-analysis", params = "stockCode")
    EarningsAnalysisSnapshot loadLatestEarningsAnalysis(@RequestParam String stockCode) {
        return earningsAnalysisQueryUseCase.findLatestByStockCode(stockCode);
    }

    @GetMapping(value = "/earnings-analysis", params = "baseDate")
    List<EarningsAnalysisSnapshot> loadEarningsAnalysisByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        return earningsAnalysisQueryUseCase.findByBaseDate(baseDate);
    }

    @PostMapping("/earnings-events")
    EarningsEvent createEarningsEvent(@Valid @RequestBody EarningsEventRequest request) {
        return earningsEventUseCase.create(request.toCreateCommand());
    }

    @GetMapping(value = "/earnings-events", params = {"from", "to"})
    List<EarningsEvent> findEarningsEventsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return earningsEventUseCase.find(null, from, to);
    }

    @GetMapping(value = "/earnings-events", params = "stockCode")
    List<EarningsEvent> findEarningsEventsByStock(@RequestParam String stockCode) {
        return earningsEventUseCase.find(stockCode, null, null);
    }

    @PatchMapping("/earnings-events/{id}")
    EarningsEvent updateEarningsEvent(
            @PathVariable long id,
            @RequestBody EarningsEventPatchRequest request
    ) {
        return earningsEventUseCase.update(id, request.toCommand());
    }

    @PostMapping("/earnings-previews")
    EarningsPreview createEarningsPreview(@Valid @RequestBody EarningsPreviewRequest request) {
        return earningsPreviewUseCase.create(request.toCommand());
    }

    @PostMapping("/earnings-previews/generate")
    EarningsPreview generateEarningsPreview(
            @RequestParam String stockCode,
            @RequestParam long earningsEventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate previewDate
    ) {
        return generateEarningsPreviewUseCase.generate(stockCode, earningsEventId, previewDate);
    }

    @GetMapping("/earnings-previews")
    List<EarningsPreview> findEarningsPreviews(@RequestParam String stockCode) {
        return earningsPreviewUseCase.findByStockCode(stockCode);
    }

    @GetMapping("/earnings-previews/upcoming")
    List<EarningsPreview> findUpcomingEarningsPreviews(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return earningsPreviewUseCase.findUpcomingReady(from, to);
    }

    @PostMapping("/post-earnings-reviews")
    PostEarningsReview createPostEarningsReview(@Valid @RequestBody PostEarningsReviewRequest request) {
        return postEarningsReviewUseCase.create(request.toCommand());
    }

    @GetMapping("/post-earnings-reviews")
    List<PostEarningsReview> findPostEarningsReviews(@RequestParam String stockCode) {
        return postEarningsReviewUseCase.findByStockCode(stockCode);
    }

    @PostMapping("/dart/corp-mappings")
    DartCorpMapping saveDartCorpMapping(@Valid @RequestBody DartCorpMappingRequest request) {
        return dartCorpMappingUseCase.save(request.toCommand());
    }

    @GetMapping(value = "/dart/corp-mappings", params = "stockCode")
    DartCorpMapping findDartCorpMapping(@RequestParam String stockCode) {
        return dartCorpMappingUseCase.findByStockCode(stockCode)
                .orElseThrow(() -> new ResearchNotFoundException("DART corp mapping not found: " + stockCode));
    }

    @GetMapping("/dart/corp-mappings")
    List<DartCorpMapping> findDartCorpMappings() {
        return dartCorpMappingUseCase.findAll();
    }

    @PostMapping("/dart/corp-codes/import")
    DartCorpCodeImportHistory importDartCorpCodes() {
        return dartCorpCodeImportUseCase.importCorpCodes();
    }

    @GetMapping("/dart/corp-codes/import-histories")
    List<DartCorpCodeImportHistory> findDartCorpCodeImportHistories() {
        return dartCorpCodeImportUseCase.findCorpCodeImportHistories();
    }

    @PostMapping("/dart/financials/import")
    DartFinancialImportHistory importDartFinancial(
            @RequestParam String stockCode,
            @RequestParam int fiscalYear,
            @RequestParam String reportCode
    ) {
        return importDartFinancialsUseCase.importStock(stockCode, fiscalYear, reportCode);
    }

    @PostMapping("/dart/financials/import-recent")
    List<DartFinancialImportHistory> importRecentDartFinancials(
            @RequestParam String stockCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        return importDartFinancialsUseCase.importStockRecent(stockCode, baseDate);
    }

    @PostMapping("/dart/financials/import-watchlist")
    List<DartFinancialImportHistory> importWatchlistDartFinancials(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        return importDartFinancialsUseCase.importActiveWatchlist(baseDate);
    }

    @GetMapping("/dart/financials/import-histories")
    List<DartFinancialImportHistory> findDartFinancialImportHistories(@RequestParam String stockCode) {
        return dartHistoryQueryUseCase.findByStockCode(stockCode);
    }

    public record ThesisRequest(
            @NotBlank String stockCode,
            @NotBlank String title,
            @NotBlank String coreAssumption,
            @NotBlank String invalidationCondition,
            @PositiveOrZero BigDecimal targetPrice,
            @NotBlank String stopLossCondition,
            @Min(0) @Max(100) int confidence,
            ThesisStatus status
    ) {
        CreateThesisCommand toCreateCommand() {
            return new CreateThesisCommand(stockCode, title, coreAssumption,
                    invalidationCondition, targetPrice, stopLossCondition, confidence, status);
        }
    }

    public record ThesisPatchRequest(
            String title,
            String coreAssumption,
            String invalidationCondition,
            @PositiveOrZero BigDecimal targetPrice,
            String stopLossCondition,
            @Min(0) @Max(100) Integer confidence,
            ThesisStatus status
    ) {
        UpdateThesisCommand toCommand() {
            return new UpdateThesisCommand(title, coreAssumption, invalidationCondition,
                    targetPrice, stopLossCondition, confidence, status);
        }
    }

    public record CatalystRequest(
            String stockCode,
            @NotBlank String title,
            @NotNull CatalystType catalystType,
            @NotNull LocalDate expectedDate,
            @NotNull CatalystImportance importance,
            CatalystStatus status,
            String sourceUrl,
            String memo
    ) {
        CreateCatalystCommand toCreateCommand() {
            return new CreateCatalystCommand(stockCode, title, catalystType, expectedDate,
                    importance, status, sourceUrl, memo);
        }
    }

    public record CatalystPatchRequest(
            String stockCode,
            String title,
            CatalystType catalystType,
            LocalDate expectedDate,
            CatalystImportance importance,
            CatalystStatus status,
            String sourceUrl,
            String memo
    ) {
        UpdateCatalystCommand toCommand() {
            return new UpdateCatalystCommand(stockCode, title, catalystType, expectedDate,
                    importance, status, sourceUrl, memo);
        }
    }

    public record SectorRequest(
            @NotBlank String sectorCode,
            @NotBlank String sectorName,
            SectorType sectorType
    ) {
        CreateSectorCommand toCommand() {
            return new CreateSectorCommand(sectorCode, sectorName, sectorType);
        }
    }

    public record SectorStockRequest(
            @NotBlank String stockCode,
            String source
    ) {
        AddSectorStockCommand toCommand() {
            return new AddSectorStockCommand(stockCode, source);
        }
    }

    public record QuarterlyFinancialRequest(
            @NotBlank String stockCode,
            @Min(1900) int fiscalYear,
            @Min(1) @Max(4) int fiscalQuarter,
            @NotNull BigDecimal revenue,
            @NotNull BigDecimal operatingIncome,
            @NotNull BigDecimal netIncome,
            @NotNull BigDecimal totalAssets,
            @NotNull BigDecimal totalLiabilities,
            @NotNull BigDecimal totalEquity,
            @NotNull BigDecimal operatingCashFlow,
            @NotNull BigDecimal freeCashFlow
    ) {
        CreateQuarterlyFinancialCommand toCommand() {
            return new CreateQuarterlyFinancialCommand(stockCode, fiscalYear, fiscalQuarter,
                    revenue, operatingIncome, netIncome, totalAssets, totalLiabilities,
                    totalEquity, operatingCashFlow, freeCashFlow);
        }
    }

    public record ValuationSnapshotRequest(
            @NotBlank String stockCode,
            @NotNull LocalDate tradeDate,
            @NotNull BigDecimal marketCap,
            BigDecimal per,
            BigDecimal pbr,
            BigDecimal psr,
            BigDecimal eps,
            BigDecimal bps,
            BigDecimal salesPerShare,
            ValuationSnapshotSource source
    ) {
        CreateValuationSnapshotCommand toCommand() {
            return new CreateValuationSnapshotCommand(stockCode, tradeDate, marketCap,
                    per, pbr, psr, eps, bps, salesPerShare, source);
        }
    }

    public record SharesOutstandingRequest(
            @NotBlank String stockCode,
            @NotNull LocalDate baseDate,
            @NotNull @Positive BigDecimal sharesOutstanding,
            SharesOutstandingSource source
    ) {
        SaveSharesOutstandingCommand toCommand() {
            return new SaveSharesOutstandingCommand(stockCode, baseDate, sharesOutstanding,
                    source == null ? SharesOutstandingSource.MANUAL : source);
        }
    }

    public record ValuationGenerationBatchRequest(
            @NotEmpty List<@NotBlank String> stockCodes
    ) {
    }

    public record EarningsAnalysisBatchRequest(
            @NotEmpty List<@NotBlank String> stockCodes
    ) {
    }

    public record EarningsEventRequest(
            @NotBlank String stockCode,
            @Min(1900) int fiscalYear,
            @Min(1) @Max(4) int fiscalQuarter,
            @NotNull LocalDate expectedAnnouncementDate,
            LocalDate actualAnnouncementDate,
            EarningsEventStatus status,
            String memo,
            Boolean autoCreateCatalyst
    ) {
        CreateEarningsEventCommand toCreateCommand() {
            return new CreateEarningsEventCommand(stockCode, fiscalYear, fiscalQuarter,
                    expectedAnnouncementDate, actualAnnouncementDate, status, memo,
                    autoCreateCatalyst);
        }
    }

    public record EarningsEventPatchRequest(
            LocalDate expectedAnnouncementDate,
            LocalDate actualAnnouncementDate,
            EarningsEventStatus status,
            String memo
    ) {
        UpdateEarningsEventCommand toCommand() {
            return new UpdateEarningsEventCommand(expectedAnnouncementDate,
                    actualAnnouncementDate, status, memo);
        }
    }

    public record EarningsPreviewRequest(
            @Positive long earningsEventId,
            @NotBlank String stockCode,
            @NotNull LocalDate previewDate,
            List<String> keyCheckpoints,
            BigDecimal expectedRevenue,
            BigDecimal expectedOperatingIncome,
            BigDecimal expectedNetIncome,
            BigDecimal expectedOperatingMargin,
            List<String> expectedRisks,
            List<String> thesisWatchPoints,
            EarningsPreviewStatus status
    ) {
        CreateEarningsPreviewCommand toCommand() {
            return new CreateEarningsPreviewCommand(earningsEventId, stockCode,
                    previewDate, keyCheckpoints, expectedRevenue, expectedOperatingIncome,
                    expectedNetIncome, expectedOperatingMargin, expectedRisks,
                    thesisWatchPoints, status);
        }
    }

    public record PostEarningsReviewRequest(
            @Positive long earningsEventId,
            @NotBlank String stockCode,
            @NotNull LocalDate reviewDate,
            @NotNull BigDecimal actualRevenue,
            @NotNull BigDecimal actualOperatingIncome,
            @NotNull BigDecimal actualNetIncome,
            BigDecimal actualOperatingMargin,
            @NotNull ThesisImpact thesisImpact,
            @NotBlank String reviewSummary,
            List<String> actionItems,
            boolean upsertQuarterlyFinancial,
            boolean rerunEarningsAnalysis
    ) {
        CreatePostEarningsReviewCommand toCommand() {
            return new CreatePostEarningsReviewCommand(earningsEventId, stockCode,
                    reviewDate, actualRevenue, actualOperatingIncome, actualNetIncome,
                    actualOperatingMargin, thesisImpact, reviewSummary, actionItems,
                    upsertQuarterlyFinancial, rerunEarningsAnalysis);
        }
    }

    public record DartCorpMappingRequest(
            @NotBlank String stockCode,
            @NotBlank String corpCode,
            @NotBlank String corpName,
            @NotNull seokhoon.trade.domain.stock.Market market
    ) {
        SaveDartCorpMappingCommand toCommand() {
            return new SaveDartCorpMappingCommand(stockCode, corpCode, corpName, market);
        }
    }
}
