package seokhoon.trade.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import seokhoon.trade.domain.kis.KisEnvironment;

public interface KisAccountBalancePort {
    List<AccountHolding> holdings(long accountId);

    record AccountHolding(KisEnvironment environment, String stockCode, String stockName, int quantity,
            BigDecimal averageBuyPrice, BigDecimal buyAmount,
            BigDecimal currentPrice, BigDecimal marketValue,
            BigDecimal unrealizedProfitLoss, BigDecimal unrealizedReturnRate) {}
}
