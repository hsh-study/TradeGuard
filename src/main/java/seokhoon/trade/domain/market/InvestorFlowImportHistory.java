package seokhoon.trade.domain.market;

import java.time.Instant;
import java.time.LocalDate;

public record InvestorFlowImportHistory(Long id, InvestorFlowImportScope scope,
        String stockCode, InvestorFlowMarket market, LocalDate tradeDate, InvestorFlowProvider provider,
        InvestorFlowImportStatus status, int importedCount, String failureReason,
        Instant requestedAt, Instant completedAt) {
}
