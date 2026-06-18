package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StockSupplyDemandSnapshot(Long id, String stockCode, LocalDate tradeDate,
        BigDecimal foreignNetBuyAmount, BigDecimal institutionNetBuyAmount,
        BigDecimal individualNetBuyAmount, int consecutiveForeignBuyDays,
        int consecutiveInstitutionBuyDays, int consecutiveCombinedSmartMoneyBuyDays,
        BigDecimal smartMoneyNetBuyAmount, BigDecimal smartMoney5dayNetBuyAmount,
        BigDecimal individualDominanceRatio, int supplyDemandScore,
        SupplyDemandStatus status, List<String> reasons, Instant createdAt, Instant updatedAt) {
}
