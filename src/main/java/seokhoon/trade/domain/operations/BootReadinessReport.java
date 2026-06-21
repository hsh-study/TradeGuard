package seokhoon.trade.domain.operations;

import java.time.Instant;
import java.util.List;

public record BootReadinessReport(
        Instant checkedAt,
        String profile,
        String environment,
        OverallStatus overallStatus,
        String applicationVersion,
        ComponentStatus databaseStatus,
        ComponentStatus flywayStatus,
        ComponentStatus actuatorStatus,
        ComponentStatus kisStatus,
        ComponentStatus dartStatus,
        ComponentStatus investorFlowStatus,
        ComponentStatus disclosureStatus,
        ComponentStatus consensusStatus,
        ComponentStatus schedulerStatus,
        ComponentStatus liveTradingStatus,
        ComponentStatus observabilityStatus,
        List<String> blockingIssues,
        List<String> warnings,
        List<String> recommendedActions
) {
    public BootReadinessReport {
        blockingIssues = List.copyOf(blockingIssues);
        warnings = List.copyOf(warnings);
        recommendedActions = List.copyOf(recommendedActions);
    }

    public enum OverallStatus { READY, WARNING, BLOCKED }

    public record ComponentStatus(String status, List<String> facts) {
        public ComponentStatus {
            facts = List.copyOf(facts);
        }
    }
}
