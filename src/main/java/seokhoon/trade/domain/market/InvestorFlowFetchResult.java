package seokhoon.trade.domain.market;

import java.util.List;

public record InvestorFlowFetchResult<T>(List<T> flows, int rejectedCount) {
    public InvestorFlowFetchResult {
        flows = flows == null ? List.of() : List.copyOf(flows);
        if (rejectedCount < 0) {
            throw new IllegalArgumentException("rejectedCount must be non-negative");
        }
    }

    public static <T> InvestorFlowFetchResult<T> success(List<T> flows) {
        return new InvestorFlowFetchResult<>(flows, 0);
    }

    public static <T> InvestorFlowFetchResult<T> empty() {
        return new InvestorFlowFetchResult<>(List.of(), 0);
    }
}
