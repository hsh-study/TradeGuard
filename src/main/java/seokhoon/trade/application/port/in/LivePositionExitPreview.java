package seokhoon.trade.application.port.in;

import java.math.BigDecimal;

public record LivePositionExitPreview(
        BigDecimal buyAmount,
        BigDecimal grossSellAmount,
        BigDecimal estimatedSellTax,
        BigDecimal estimatedBuyCommission,
        BigDecimal estimatedSellCommission,
        BigDecimal estimatedNetProfit,
        BigDecimal estimatedNetReturnRate,
        boolean takeProfitTriggered,
        boolean stopLossTriggered,
        boolean maxLossTriggered,
        LiveExitAction suggestedAction
) {
}
