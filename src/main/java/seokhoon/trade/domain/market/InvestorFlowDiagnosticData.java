package seokhoon.trade.domain.market;

import java.util.List;
import java.util.Map;

public record InvestorFlowDiagnosticData(
        String endpoint,
        String trId,
        int detectedRows,
        List<String> availableFields,
        List<String> sampleInvestorTypes,
        Map<String, String> rawAmountFieldsMasked,
        Map<String, String> rawQuantityFieldsMasked,
        boolean requestedTradeDateFound
) {
    public InvestorFlowDiagnosticData {
        availableFields = List.copyOf(availableFields);
        sampleInvestorTypes = List.copyOf(sampleInvestorTypes);
        rawAmountFieldsMasked = Map.copyOf(rawAmountFieldsMasked);
        rawQuantityFieldsMasked = Map.copyOf(rawQuantityFieldsMasked);
    }
}
