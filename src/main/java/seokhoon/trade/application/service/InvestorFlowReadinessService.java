package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.GetInvestorFlowReadinessUseCase;
import seokhoon.trade.application.port.in.InvestorFlowReadiness;
import seokhoon.trade.application.port.out.InvestorFlowImportHistoryPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryRecord;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.config.InvestorFlowProperties;
import seokhoon.trade.domain.market.InvestorFlowImportHistory;
import seokhoon.trade.domain.market.InvestorFlowImportScope;
import seokhoon.trade.domain.market.InvestorFlowImportStatus;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.util.ArrayList;
import java.util.List;

@Service
public class InvestorFlowReadinessService implements GetInvestorFlowReadinessUseCase {
    private final InvestorFlowProperties properties;
    private final InvestorFlowImportHistoryPort importHistories;
    private final SchedulerExecutionHistoryPort schedulerHistories;
    private final OperationalMetricsPort metrics;

    public InvestorFlowReadinessService(InvestorFlowProperties properties,
            InvestorFlowImportHistoryPort importHistories,
            SchedulerExecutionHistoryPort schedulerHistories,
            OperationalMetricsPort metrics) {
        this.properties = properties;
        this.importHistories = importHistories;
        this.schedulerHistories = schedulerHistories;
        this.metrics = metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public InvestorFlowReadiness getReadiness() {
        List<InvestorFlowImportHistory> recentImports = importHistories.findRecent(null, 100);
        InvestorFlowImportStatus stockStatus = latestImportStatus(recentImports, InvestorFlowImportScope.STOCK);
        InvestorFlowImportStatus marketStatus = latestImportStatus(recentImports, InvestorFlowImportScope.MARKET);
        SchedulerExecutionStatus analysisStatus = schedulerHistories
                .find(null, SchedulerName.SUPPLY_DEMAND_ANALYSIS, null).stream()
                .findFirst()
                .map(SchedulerExecutionHistoryRecord::status)
                .orElse(null);

        List<String> blocking = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        evaluateConfiguration(blocking, warnings, actions);
        if (properties.isProviderEnabled()) {
            evaluateHistory(stockStatus, marketStatus, analysisStatus, blocking, warnings, actions);
        }

        boolean ready = blocking.isEmpty();
        metrics.recordInvestorFlowReadiness(ready ? "ready" : "not_ready");
        return new InvestorFlowReadiness(
                properties.isProviderEnabled(),
                properties.getProviderType(),
                properties.getKisAmountUnit(),
                properties.isKisAmountUnitVerified(),
                properties.isDiagnosticEnabled(),
                properties.isDiagnosticAllowHttp(),
                properties.isDiagnosticMaskResponse(),
                properties.isImportAutoRun(),
                properties.getLookbackDays(),
                stockStatus,
                marketStatus,
                analysisStatus,
                ready,
                blocking,
                warnings,
                actions
        );
    }

    private void evaluateConfiguration(List<String> blocking, List<String> warnings,
            List<String> actions) {
        if (!properties.isProviderEnabled()) {
            warnings.add("INVESTOR_FLOW_PROVIDER_DISABLED");
            actions.add("Enable INVESTOR_FLOW_PROVIDER_ENABLED when automatic collection is required");
            return;
        }
        if (!properties.isKisAmountUnitVerified()) {
            blocking.add("AMOUNT_UNIT_UNVERIFIED");
            actions.add("Run verify stock API");
            actions.add("Compare KIS HTS amount unit");
            actions.add("Set KIS_INVESTOR_FLOW_AMOUNT_UNIT");
            if (properties.isImportAutoRun()) {
                blocking.add("AUTO_RUN_BLOCKED_BY_UNVERIFIED_AMOUNT_UNIT");
            }
        }
        if (properties.isDiagnosticEnabled()) {
            warnings.add("DIAGNOSTIC_MODE_ENABLED");
            actions.add("Disable KIS investor flow diagnostic after verification");
        }
        if (properties.isDiagnosticAllowHttp()) {
            warnings.add("DIAGNOSTIC_HTTP_ENABLED");
        }
        if (!properties.isImportAutoRun()) {
            warnings.add("INVESTOR_FLOW_AUTO_RUN_DISABLED");
        }
    }

    private static void evaluateHistory(InvestorFlowImportStatus stockStatus,
            InvestorFlowImportStatus marketStatus, SchedulerExecutionStatus analysisStatus,
            List<String> blocking, List<String> warnings, List<String> actions) {
        if (stockStatus == null) {
            warnings.add("STOCK_IMPORT_HISTORY_UNAVAILABLE");
        } else if (stockStatus == InvestorFlowImportStatus.FAILED) {
            blocking.add("LATEST_STOCK_IMPORT_FAILED");
            actions.add("Review latest stock investor flow import failure");
        }
        if (marketStatus == null) {
            warnings.add("MARKET_IMPORT_HISTORY_UNAVAILABLE");
        } else if (marketStatus == InvestorFlowImportStatus.FAILED) {
            blocking.add("LATEST_MARKET_IMPORT_FAILED");
            actions.add("Review latest market investor flow import failure");
        }
        if (analysisStatus == null) {
            warnings.add("SUPPLY_DEMAND_ANALYSIS_HISTORY_UNAVAILABLE");
        } else if (analysisStatus == SchedulerExecutionStatus.FAILED) {
            blocking.add("LATEST_SUPPLY_DEMAND_ANALYSIS_FAILED");
            actions.add("Review latest supply-demand analysis failure");
        }
    }

    private static InvestorFlowImportStatus latestImportStatus(
            List<InvestorFlowImportHistory> histories, InvestorFlowImportScope scope) {
        return histories.stream()
                .filter(history -> history.scope() == scope)
                .findFirst()
                .map(InvestorFlowImportHistory::status)
                .orElse(null);
    }
}
