package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

public interface OperationalMetricsPort {
    void recordSchedulerExecution(
            SchedulerName schedulerName,
            SchedulerExecutionStatus status
    );

    void recordSchedulerSelected(SchedulerName schedulerName, int selectedCount);

    void recordSchedulerNotification(SchedulerName schedulerName, boolean sent);

    void recordOrderRequest(String status);

    void recordBrokerFailure(boolean retryable);

    void recordOrderRetry(String result);

    void recordOrderRetryRecovery(String result);

    void recordDiscordNotification(String result);

    void recordKisReadOnly(String operation, String result);
    void recordKisTokenIssue(String environment, String result);
    void recordKisTokenCache(String environment, String result);
    void recordKisTokenStore(String cacheMode, String result);

    void recordAfterHoursLookup(String result);

    void recordIntradayBarLookup(String result);

    void recordEarlyMarketPerformanceCapture(String result);

    void recordEarlyMarketFollowUp(String decision);

    void recordEarlyMarketPriceAction(String result);

    void recordEarlyMarketReport(String result);

    void recordEarlyMarketPeriodReport(String result);

    void recordEarlyMarketExperiment(String result);

    void recordEarlyMarketExperimentCompare(String result);

    void recordEarlyMarketBacktest(String result);

    void recordEarlyMarketFollowUpPersist(String result);

    void recordMarketCalendarSync(String result, int year);

    void recordMarketCalendarLookup(String result, String market);

    void recordMarketCalendarOverride(String result);

    void recordMarketCalendarValidation(String result);

    void recordEarlyMarketDataCapture(String captureType, String result);

    void recordLiveOrderRequest(String side, String status);

    void recordLiveOrderSubmit(String side, String result);

    void recordLivePositionExitEvaluation(String result);
    void recordLiveOrderReconciliation(String result);
    void recordLiveOrderCancel(String result);
    void recordLiveTradingReadiness(String result);
    void recordIndicatorWarmUp(String result);
    void recordIndicatorDataSufficiency(String result);
    void recordResearchThesis(String status);
    void recordResearchCatalyst(String status);
    void recordResearchMorningNote(String result);
    void recordResearchSectorSnapshot(String result);
    void recordResearchEarningsAnalysis(String result);
    void recordResearchFinancialImport(String result);
    void recordResearchValuationImport(String result);
    void recordResearchValuationAutoSnapshot(String result);
    void recordResearchSharesOutstanding(String result);
    void recordResearchEarningsEvent(String status);
    void recordResearchEarningsPreview(String result);
    void recordResearchPostEarningsReview(String thesisImpact);
    void recordDartFinancialImport(String result);
    void recordDartProvider(String operation, String result);
    void recordDartCorpCodeImport(String result);
    void recordSharesOutstandingImport(String result);
    void recordCatalystEvidence(String type, String confidence);
    void recordDisclosureEvidenceImport(String provider, String result);
    void recordMarketIndexImport(String provider, String result);
    void recordSectorImport(String result);

    static OperationalMetricsPort noop() {
        return new OperationalMetricsPort() {
            @Override
            public void recordSchedulerExecution(
                    SchedulerName schedulerName,
                    SchedulerExecutionStatus status
            ) {
            }

            @Override
            public void recordSchedulerSelected(
                    SchedulerName schedulerName,
                    int selectedCount
            ) {
            }

            @Override
            public void recordSchedulerNotification(
                    SchedulerName schedulerName,
                    boolean sent
            ) {
            }

            @Override
            public void recordOrderRequest(String status) {
            }

            @Override
            public void recordBrokerFailure(boolean retryable) {
            }

            @Override
            public void recordOrderRetry(String result) {
            }

            @Override
            public void recordOrderRetryRecovery(String result) {
            }

            @Override
            public void recordDiscordNotification(String result) {
            }

            @Override
            public void recordKisReadOnly(String operation, String result) {
            }

            @Override
            public void recordKisTokenIssue(String environment, String result) {
            }

            @Override
            public void recordKisTokenCache(String environment, String result) {
            }

            @Override
            public void recordKisTokenStore(String cacheMode, String result) {
            }

            @Override
            public void recordAfterHoursLookup(String result) {
            }

            @Override
            public void recordIntradayBarLookup(String result) {
            }

            @Override
            public void recordEarlyMarketPerformanceCapture(String result) {
            }

            @Override
            public void recordEarlyMarketFollowUp(String decision) {
            }

            @Override
            public void recordEarlyMarketPriceAction(String result) {
            }

            @Override
            public void recordEarlyMarketReport(String result) {
            }

            @Override
            public void recordEarlyMarketPeriodReport(String result) {
            }

            @Override
            public void recordEarlyMarketExperiment(String result) {
            }

            @Override
            public void recordEarlyMarketExperimentCompare(String result) {
            }

            @Override
            public void recordEarlyMarketBacktest(String result) {
            }

            @Override
            public void recordEarlyMarketFollowUpPersist(String result) {
            }

            @Override
            public void recordMarketCalendarSync(String result, int year) {
            }

            @Override
            public void recordMarketCalendarLookup(String result, String market) {
            }

            @Override
            public void recordMarketCalendarOverride(String result) {
            }

            @Override
            public void recordMarketCalendarValidation(String result) {
            }

            @Override
            public void recordEarlyMarketDataCapture(
                    String captureType,
                    String result
            ) {
            }

            @Override public void recordLiveOrderRequest(String side, String status) {}
            @Override public void recordLiveOrderSubmit(String side, String result) {}
            @Override public void recordLivePositionExitEvaluation(String result) {}
            @Override public void recordLiveOrderReconciliation(String result) {}
            @Override public void recordLiveOrderCancel(String result) {}
            @Override public void recordLiveTradingReadiness(String result) {}
            @Override public void recordIndicatorWarmUp(String result) {}
            @Override public void recordIndicatorDataSufficiency(String result) {}
            @Override public void recordResearchThesis(String status) {}
            @Override public void recordResearchCatalyst(String status) {}
            @Override public void recordResearchMorningNote(String result) {}
            @Override public void recordResearchSectorSnapshot(String result) {}
            @Override public void recordResearchEarningsAnalysis(String result) {}
            @Override public void recordResearchFinancialImport(String result) {}
            @Override public void recordResearchValuationImport(String result) {}
            @Override public void recordResearchValuationAutoSnapshot(String result) {}
            @Override public void recordResearchSharesOutstanding(String result) {}
            @Override public void recordResearchEarningsEvent(String status) {}
            @Override public void recordResearchEarningsPreview(String result) {}
            @Override public void recordResearchPostEarningsReview(String thesisImpact) {}
            @Override public void recordDartFinancialImport(String result) {}
            @Override public void recordDartProvider(String operation, String result) {}
            @Override public void recordDartCorpCodeImport(String result) {}
            @Override public void recordSharesOutstandingImport(String result) {}
            @Override public void recordCatalystEvidence(String type, String confidence) {}
            @Override public void recordDisclosureEvidenceImport(String provider, String result) {}
            @Override public void recordMarketIndexImport(String provider, String result) {}
            @Override public void recordSectorImport(String result) {}
        };
    }
}
