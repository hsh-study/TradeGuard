package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.InvestorFlowImportStatus;
import seokhoon.trade.domain.market.KisInvestorFlowAmountUnit;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;

import java.util.List;

public record InvestorFlowReadiness(
        boolean providerEnabled,
        String providerType,
        KisInvestorFlowAmountUnit amountUnit,
        boolean amountUnitVerified,
        boolean diagnosticEnabled,
        boolean diagnosticAllowHttp,
        boolean diagnosticMaskResponse,
        boolean importAutoRun,
        int lookbackDays,
        InvestorFlowImportStatus latestStockImportStatus,
        InvestorFlowImportStatus latestMarketImportStatus,
        SchedulerExecutionStatus latestSupplyDemandAnalysisStatus,
        boolean ready,
        List<String> blockingReasons,
        List<String> warnings,
        List<String> recommendedNextActions
) {
    public InvestorFlowReadiness {
        blockingReasons = List.copyOf(blockingReasons);
        warnings = List.copyOf(warnings);
        recommendedNextActions = List.copyOf(recommendedNextActions);
    }
}
