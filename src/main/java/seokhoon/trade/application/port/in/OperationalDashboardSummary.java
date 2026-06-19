package seokhoon.trade.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OperationalDashboardSummary(
        LocalDate baseDate,
        MarketDateStatus marketDateStatus,
        MorningNoteStatus morningNoteStatus,
        EarlyMarketStatus earlyMarketStatus,
        ClosingBetStatus closingBetStatus,
        InvestorFlowStatus investorFlowStatus,
        EarningsStatus earningsStatus,
        DartStatus dartStatus,
        ConsensusStatus consensusStatus,
        ValuationStatus valuationStatus,
        PaperTradingReportStatus paperTradingReportStatus,
        ReplayBacktestStatus replayBacktestStatus,
        SchedulerStatus schedulerStatus,
        KisTokenStatus kisTokenStatus,
        LiveTradingReadinessStatus liveTradingReadinessStatus,
        List<String> blockingIssues,
        List<String> warnings,
        List<String> recommendedActions,
        Instant generatedAt
) {
    public OperationalDashboardSummary {
        blockingIssues = List.copyOf(blockingIssues);
        warnings = List.copyOf(warnings);
        recommendedActions = List.copyOf(recommendedActions);
    }

    public record MarketDateStatus(LocalDate baseDate, boolean isTradingDay,
            LocalDate previousTradingDay, LocalDate nextTradingDay, String calendarSource,
            List<String> warnings) { public MarketDateStatus { warnings = List.copyOf(warnings); } }

    public record MorningNoteStatus(boolean generated, LocalDate tradeDate, int actionItemCount,
            int criticalActionItemCount, Instant latestGeneratedAt, boolean discordEnabled,
            List<String> warnings) { public MorningNoteStatus { warnings = List.copyOf(warnings); } }

    public record EarlyMarketStatus(int preOpenCandidateCount, int compressedCandidateCount,
            int followUpCount, int performanceCaptureCount, String dataCaptureStatus,
            String latestRunStatus, List<String> warnings) {
        public EarlyMarketStatus { warnings = List.copyOf(warnings); }
    }

    public record ClosingBetStatus(int preScanCandidateCount, int finalCandidateCount,
            String latestPreScanStatus, String latestFinalReviewStatus, List<String> warnings) {
        public ClosingBetStatus { warnings = List.copyOf(warnings); }
    }

    public record InvestorFlowStatus(boolean readinessReady, boolean providerEnabled, String amountUnit,
            boolean amountUnitVerified, String latestStockImportStatus, String latestMarketImportStatus,
            String latestSupplyDemandAnalysisStatus, int strongAccumulationCount, int distributionCount,
            List<String> warnings) { public InvestorFlowStatus { warnings = List.copyOf(warnings); } }

    public record EarningsStatus(int earningsAnalysisCount, int strongCount, int weakCount,
            int dataInsufficientCount, int upcomingEarningsCount, int reviewRequiredCount,
            List<String> warnings) { public EarningsStatus { warnings = List.copyOf(warnings); } }

    public record DartStatus(boolean providerEnabled, String latestFinancialImportStatus,
            String latestCorpCodeImportStatus, int mappingMissingCount, int failedImportCount,
            boolean disclosureProviderEnabled, String latestDisclosureImportStatus,
            int failedDisclosureImportCount, int highImportanceDisclosureCount,
            List<String> warnings) { public DartStatus { warnings = List.copyOf(warnings); } }

    public record ConsensusStatus(int earningsConsensusCount,int targetPriceConsensusCount,
            int staleConsensusCount,int missingConsensusForUpcomingEarningsCount,List<String>warnings){
        public ConsensusStatus{warnings=List.copyOf(warnings);}}

    public record ValuationStatus(String latestAutoSnapshotStatus, int generatedCount,
            int insufficientCount, int sharesOutstandingMissingCount, List<String> warnings) {
        public ValuationStatus { warnings = List.copyOf(warnings); }
    }

    public record PaperTradingReportStatus(Long latestRunId, boolean generated, int totalCandidates,
            BigDecimal winRate, BigDecimal averageReturnRate, int dataInsufficientCount,
            List<String> warnings) { public PaperTradingReportStatus { warnings = List.copyOf(warnings); } }

    public record ReplayBacktestStatus(Long latestRunId, String latestStrategy, String latestStatus,
            BigDecimal latestWinRate, BigDecimal latestAverageReturnRate, List<String> warnings) {
        public ReplayBacktestStatus { warnings = List.copyOf(warnings); }
    }

    public record SchedulerFailure(String schedulerName, String failureReason, Instant failedAt) {}

    public record SchedulerStatus(int totalToday, int successCount, int failedCount, int skippedCount,
            List<SchedulerFailure> latestFailures, List<String> warnings) {
        public SchedulerStatus { latestFailures = List.copyOf(latestFailures); warnings = List.copyOf(warnings); }
    }

    public record KisTokenStatus(String cacheMode, String realTokenStatus, String demoTokenStatus,
            boolean refreshInProgress, List<String> warnings) {
        public KisTokenStatus { warnings = List.copyOf(warnings); }
    }

    public record LiveTradingReadinessStatus(boolean ready, boolean liveTradingEnabled,
            boolean kisTradingEnabled, boolean killSwitchEnabled, List<String> blockingReasons,
            List<String> warnings) {
        public LiveTradingReadinessStatus {
            blockingReasons = List.copyOf(blockingReasons); warnings = List.copyOf(warnings);
        }
    }
}
