package seokhoon.trade.adapter.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

@Component
public class MicrometerOperationalMetricsAdapter implements OperationalMetricsPort {
    private final MeterRegistry meterRegistry;

    public MicrometerOperationalMetricsAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordSchedulerExecution(
            SchedulerName schedulerName,
            SchedulerExecutionStatus status
    ) {
        meterRegistry.counter(
                "tradeguard.scheduler.execution.count",
                "schedulerName", schedulerName.name(),
                "status", status.name()
        ).increment();
    }

    @Override
    public void recordSchedulerSelected(SchedulerName schedulerName, int selectedCount) {
        if (selectedCount > 0) {
            meterRegistry.counter(
                    "tradeguard.scheduler.selected.count",
                    "schedulerName", schedulerName.name()
            ).increment(selectedCount);
        }
    }

    @Override
    public void recordSchedulerNotification(SchedulerName schedulerName, boolean sent) {
        meterRegistry.counter(
                "tradeguard.scheduler.notification.sent.count",
                "schedulerName", schedulerName.name(),
                "sent", Boolean.toString(sent)
        ).increment();
    }

    @Override
    public void recordOrderRequest(String status) {
        meterRegistry.counter(
                "tradeguard.order.request.count",
                "status", status
        ).increment();
    }

    @Override
    public void recordBrokerFailure(boolean retryable) {
        meterRegistry.counter(
                "tradeguard.order.broker_failure.count",
                "retryable", Boolean.toString(retryable)
        ).increment();
    }

    @Override
    public void recordOrderRetry(String result) {
        meterRegistry.counter(
                "tradeguard.order.retry.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordOrderRetryRecovery(String result) {
        meterRegistry.counter(
                "tradeguard.order.retry_recovery.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordDiscordNotification(String result) {
        meterRegistry.counter(
                "tradeguard.notification.discord.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordKisReadOnly(String operation, String result) {
        meterRegistry.counter(
                "tradeguard.kis.read_only.count",
                "operation", operation,
                "result", result
        ).increment();
    }

    @Override
    public void recordKisTokenIssue(String environment, String result) {
        meterRegistry.counter(
                "tradeguard.kis.token.issue.count",
                "environment", environment,
                "result", result
        ).increment();
    }

    @Override
    public void recordKisTokenCache(String environment, String result) {
        meterRegistry.counter(
                "tradeguard.kis.token.cache.count",
                "environment", environment,
                "result", result
        ).increment();
    }

    @Override
    public void recordKisTokenStore(String cacheMode, String result) {
        meterRegistry.counter(
                "tradeguard.kis.token.store.count",
                "cacheMode",cacheMode,
                "result",result
        ).increment();
    }

    @Override
    public void recordAfterHoursLookup(String result) {
        meterRegistry.counter(
                "tradeguard.after_hours.lookup.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordIntradayBarLookup(String result) {
        meterRegistry.counter(
                "tradeguard.intraday_bar.lookup.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketPerformanceCapture(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.performance.capture.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketFollowUp(String decision) {
        meterRegistry.counter(
                "tradeguard.early_market.follow_up.count",
                "decision", decision
        ).increment();
    }

    @Override
    public void recordEarlyMarketPriceAction(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.price_action.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketReport(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.report.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketPeriodReport(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.period_report.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketExperiment(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.experiment.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketExperimentCompare(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.experiment.compare.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketBacktest(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.backtest.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordReplayBacktest(String strategy, String result) {
        meterRegistry.counter(
                "tradeguard.research.replay_backtest.count",
                "strategy", strategy,
                "result", result
        ).increment();
    }

    @Override
    public void recordPaperTradingReport(String result) {
        meterRegistry.counter(
                "tradeguard.research.paper_trading_report.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketFollowUpPersist(String result) {
        meterRegistry.counter(
                "tradeguard.early_market.follow_up.persist.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordMarketCalendarSync(String result, int year) {
        meterRegistry.counter(
                "tradeguard.market_calendar.sync.count",
                "result", result,
                "year", Integer.toString(year),
                "market", "KRX_STOCK"
        ).increment();
    }

    @Override
    public void recordMarketCalendarLookup(String result, String market) {
        meterRegistry.counter(
                "tradeguard.market_calendar.lookup.count",
                "result", result,
                "market", market
        ).increment();
    }

    @Override
    public void recordMarketCalendarOverride(String result) {
        meterRegistry.counter(
                "tradeguard.market_calendar.override.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordMarketCalendarValidation(String result) {
        meterRegistry.counter(
                "tradeguard.market_calendar.validation.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordEarlyMarketDataCapture(
            String captureType,
            String result
    ) {
        meterRegistry.counter(
                "tradeguard.early_market.data_capture.count",
                "captureType", captureType,
                "result", result
        ).increment();
    }

    @Override
    public void recordResearchEarningsAnalysis(String result) {
        meterRegistry.counter(
                "tradeguard.research.earnings_analysis.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordResearchFinancialImport(String result) {
        meterRegistry.counter(
                "tradeguard.research.financial_import.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordResearchValuationImport(String result) {
        meterRegistry.counter(
                "tradeguard.research.valuation_import.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordResearchValuationAutoSnapshot(String result) {
        meterRegistry.counter(
                "tradeguard.research.valuation_auto_snapshot.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordResearchSharesOutstanding(String result) {
        meterRegistry.counter(
                "tradeguard.research.shares_outstanding.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordResearchEarningsEvent(String status) {
        meterRegistry.counter(
                "tradeguard.research.earnings_event.count",
                "status", status
        ).increment();
    }

    @Override
    public void recordResearchEarningsPreview(String result) {
        meterRegistry.counter(
                "tradeguard.research.earnings_preview.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordResearchPostEarningsReview(String thesisImpact) {
        meterRegistry.counter(
                "tradeguard.research.post_earnings_review.count",
                "thesisImpact", thesisImpact
        ).increment();
    }

    @Override
    public void recordDartFinancialImport(String result) {
        meterRegistry.counter(
                "tradeguard.research.dart_financial_import.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordDartProvider(String operation, String result) {
        meterRegistry.counter(
                "tradeguard.research.dart_provider.count",
                "operation", operation,
                "result", result
        ).increment();
    }

    @Override
    public void recordDartCorpCodeImport(String result) {
        meterRegistry.counter(
                "tradeguard.research.dart_corp_code_import.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordSharesOutstandingImport(String result) {
        meterRegistry.counter(
                "tradeguard.research.shares_outstanding_import.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordCatalystEvidence(String type, String confidence) {
        meterRegistry.counter(
                "tradeguard.research.catalyst_evidence.count",
                "type", type,
                "confidence", confidence
        ).increment();
    }

    @Override
    public void recordDisclosureEvidenceImport(String provider, String result) {
        meterRegistry.counter(
                "tradeguard.research.disclosure_evidence_import.count",
                "provider", provider,
                "result", result
        ).increment();
    }

    @Override
    public void recordMarketIndexImport(String provider, String result) {
        meterRegistry.counter(
                "tradeguard.research.market_index_import.count",
                "provider", provider,
                "result", result
        ).increment();
    }

    @Override
    public void recordSectorImport(String result) {
        meterRegistry.counter(
                "tradeguard.research.sector_import.count",
                "result", result
        ).increment();
    }

    @Override
    public void recordInvestorFlowImport(String scope, String result) {
        meterRegistry.counter("tradeguard.research.investor_flow_import.count",
                "scope", scope, "result", result).increment();
    }

    @Override
    public void recordInvestorFlowDiagnostic(String scope, String result) {
        meterRegistry.counter("tradeguard.research.investor_flow_diagnostic.count",
                "scope", scope, "result", result).increment();
    }

    @Override
    public void recordInvestorFlowReadiness(String result) {
        meterRegistry.counter("tradeguard.research.investor_flow_readiness.count",
                "result", result).increment();
    }

    @Override
    public void recordSupplyDemandAnalysis(String result) {
        meterRegistry.counter("tradeguard.research.supply_demand_analysis.count",
                "result", result).increment();
    }

    @Override
    public void recordSupplyDemandStrategyAdjustment(String strategy, String result) {
        meterRegistry.counter("tradeguard.strategy.supply_demand_adjustment.count",
                "strategy", strategy, "result", result).increment();
    }

    @Override
    public void recordLiveOrderRequest(String side, String status) {
        meterRegistry.counter("tradeguard.live_order.request.count",
                "side", side, "status", status).increment();
    }

    @Override
    public void recordLiveOrderSubmit(String side, String result) {
        meterRegistry.counter("tradeguard.live_order.submit.count",
                "side", side, "result", result).increment();
    }

    @Override
    public void recordLivePositionExitEvaluation(String result) {
        meterRegistry.counter("tradeguard.live_position.exit_evaluation.count",
                "result", result).increment();
    }

    @Override
    public void recordLiveOrderReconciliation(String result) {
        meterRegistry.counter("tradeguard.live_order.reconciliation.count",
                "result", result).increment();
    }

    @Override
    public void recordLiveOrderCancel(String result) {
        meterRegistry.counter("tradeguard.live_order.cancel.count",
                "result", result).increment();
    }

    @Override
    public void recordLiveTradingReadiness(String result) {
        meterRegistry.counter("tradeguard.live_trading.readiness.count",
                "result",result).increment();
    }

    @Override
    public void recordIndicatorWarmUp(String result) {
        meterRegistry.counter("tradeguard.indicator.warmup.count",
                "result", result).increment();
    }

    @Override
    public void recordIndicatorDataSufficiency(String result) {
        meterRegistry.counter(
                "tradeguard.indicator.data_sufficiency.count",
                "result", result).increment();
    }

    @Override
    public void recordResearchThesis(String status) {
        meterRegistry.counter("tradeguard.research.thesis.count",
                "status", status).increment();
    }

    @Override
    public void recordResearchCatalyst(String status) {
        meterRegistry.counter("tradeguard.research.catalyst.count",
                "status", status).increment();
    }

    @Override
    public void recordResearchMorningNote(String result) {
        meterRegistry.counter("tradeguard.research.morning_note.count",
                "result", result).increment();
    }

    @Override
    public void recordResearchSectorSnapshot(String result) {
        meterRegistry.counter("tradeguard.research.sector_snapshot.count",
                "result", result).increment();
    }
}
