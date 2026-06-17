package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.market.Sector;
import seokhoon.trade.domain.market.SectorDailySnapshot;
import seokhoon.trade.domain.market.SectorType;
import seokhoon.trade.domain.market.StockSectorMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ResearchUseCases {
    private ResearchUseCases() {
    }

    public record CreateThesisCommand(
            String stockCode,
            String title,
            String coreAssumption,
            String invalidationCondition,
            BigDecimal targetPrice,
            String stopLossCondition,
            int confidence,
            ThesisStatus status
    ) {
    }

    public record UpdateThesisCommand(
            String title,
            String coreAssumption,
            String invalidationCondition,
            BigDecimal targetPrice,
            String stopLossCondition,
            Integer confidence,
            ThesisStatus status
    ) {
    }

    public interface ThesisUseCase {
        InvestmentThesis create(CreateThesisCommand command);
        List<InvestmentThesis> find(String stockCode);
        InvestmentThesis update(long id, UpdateThesisCommand command);
        InvestmentThesis close(long id);
    }

    public record CreateCatalystCommand(
            String stockCode,
            String title,
            CatalystType catalystType,
            LocalDate expectedDate,
            CatalystImportance importance,
            CatalystStatus status,
            String sourceUrl,
            String memo
    ) {
    }

    public record UpdateCatalystCommand(
            String stockCode,
            String title,
            CatalystType catalystType,
            LocalDate expectedDate,
            CatalystImportance importance,
            CatalystStatus status,
            String sourceUrl,
            String memo
    ) {
    }

    public interface CatalystUseCase {
        InvestmentCatalyst create(CreateCatalystCommand command);
        List<InvestmentCatalyst> find(String stockCode, LocalDate from, LocalDate to);
        InvestmentCatalyst update(long id, UpdateCatalystCommand command);
    }

    public interface MorningNoteUseCase {
        MorningNote generate(LocalDate tradeDate);
        MorningNote load(LocalDate tradeDate);
    }

    public record CreateSectorCommand(
            String sectorCode,
            String sectorName,
            SectorType sectorType
    ) {
    }

    public record AddSectorStockCommand(
            String stockCode,
            String source
    ) {
    }

    public record SectorSnapshotGenerationResult(
            LocalDate tradeDate,
            int sectorCount,
            int generatedCount,
            int dataInsufficientCount
    ) {
    }

    public interface SectorUseCase {
        Sector create(CreateSectorCommand command);
        StockSectorMapping addStock(String sectorCode, AddSectorStockCommand command);
        List<Sector> findAll();
        SectorDailySnapshot loadSnapshot(String sectorCode, LocalDate tradeDate);
        SectorSnapshotGenerationResult generateSnapshots(LocalDate tradeDate);
    }

    public record CreateQuarterlyFinancialCommand(
            String stockCode,
            int fiscalYear,
            int fiscalQuarter,
            BigDecimal revenue,
            BigDecimal operatingIncome,
            BigDecimal netIncome,
            BigDecimal totalAssets,
            BigDecimal totalLiabilities,
            BigDecimal totalEquity,
            BigDecimal operatingCashFlow,
            BigDecimal freeCashFlow
    ) {
    }

    public record CreateValuationSnapshotCommand(
            String stockCode,
            LocalDate tradeDate,
            BigDecimal marketCap,
            BigDecimal per,
            BigDecimal pbr,
            BigDecimal psr,
            BigDecimal eps,
            BigDecimal bps,
            BigDecimal salesPerShare
    ) {
    }

    public interface EarningsDataUseCase {
        List<QuarterlyFinancial> saveQuarterly(List<CreateQuarterlyFinancialCommand> commands);
        ValuationSnapshot saveValuation(CreateValuationSnapshotCommand command);
    }

    public interface EarningsAnalysisQueryUseCase {
        EarningsAnalysisSnapshot findLatestByStockCode(String stockCode);
        List<EarningsAnalysisSnapshot> findByBaseDate(LocalDate baseDate);
    }

    public record CreateEarningsEventCommand(
            String stockCode,
            int fiscalYear,
            int fiscalQuarter,
            LocalDate expectedAnnouncementDate,
            LocalDate actualAnnouncementDate,
            EarningsEventStatus status,
            String memo,
            Boolean autoCreateCatalyst
    ) {
    }

    public record UpdateEarningsEventCommand(
            LocalDate expectedAnnouncementDate,
            LocalDate actualAnnouncementDate,
            EarningsEventStatus status,
            String memo
    ) {
    }

    public interface EarningsEventUseCase {
        EarningsEvent create(CreateEarningsEventCommand command);
        List<EarningsEvent> find(String stockCode, LocalDate from, LocalDate to);
        EarningsEvent update(long id, UpdateEarningsEventCommand command);
    }

    public record CreateEarningsPreviewCommand(
            long earningsEventId,
            String stockCode,
            LocalDate previewDate,
            List<String> keyCheckpoints,
            BigDecimal expectedRevenue,
            BigDecimal expectedOperatingIncome,
            BigDecimal expectedNetIncome,
            BigDecimal expectedOperatingMargin,
            List<String> expectedRisks,
            List<String> thesisWatchPoints,
            EarningsPreviewStatus status
    ) {
    }

    public interface EarningsPreviewUseCase {
        EarningsPreview create(CreateEarningsPreviewCommand command);
        List<EarningsPreview> findByStockCode(String stockCode);
        List<EarningsPreview> findUpcomingReady(LocalDate from, LocalDate to);
    }

    public record CreatePostEarningsReviewCommand(
            long earningsEventId,
            String stockCode,
            LocalDate reviewDate,
            BigDecimal actualRevenue,
            BigDecimal actualOperatingIncome,
            BigDecimal actualNetIncome,
            BigDecimal actualOperatingMargin,
            ThesisImpact thesisImpact,
            String reviewSummary,
            List<String> actionItems,
            boolean upsertQuarterlyFinancial,
            boolean rerunEarningsAnalysis
    ) {
    }

    public interface PostEarningsReviewUseCase {
        PostEarningsReview create(CreatePostEarningsReviewCommand command);
        List<PostEarningsReview> findByStockCode(String stockCode);
    }
}
