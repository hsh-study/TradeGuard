package seokhoon.trade.domain.market;

import seokhoon.trade.domain.kis.KisEnvironment;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record InvestorFlowVerification(
        InvestorFlowProvider provider,
        String endpoint,
        String trId,
        KisEnvironment environment,
        LocalDate requestedTradeDate,
        int detectedRows,
        List<String> availableFields,
        List<String> sampleInvestorTypes,
        Map<String, String> rawAmountFieldsMasked,
        Map<String, String> rawQuantityFieldsMasked,
        InvestorFlowAmountUnitStatus amountUnitStatus,
        List<String> warningMessages,
        String recommendedNextAction
) {
}
