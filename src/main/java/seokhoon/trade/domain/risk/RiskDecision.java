package seokhoon.trade.domain.risk;

import java.util.List;

public record RiskDecision(boolean approved, List<String> reasons) {
    public static RiskDecision approve() {
        return new RiskDecision(true, List.of());
    }

    public static RiskDecision rejected(List<String> reasons) {
        return new RiskDecision(false, List.copyOf(reasons));
    }
}
