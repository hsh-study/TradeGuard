package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.order.LiveOrderRequest;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.application.port.in.LiveTradingUseCases.LiveSellResult;

import java.math.BigDecimal;

public interface AccountSelectedTradingUseCase {
    LiveOrderRequest buy(BuyCommand command);
    LiveSellResult sell(SellCommand command);

    record BuyCommand(long accountId, Long signalId, String stockCode,
            int quantity, BigDecimal orderPrice, OrderType orderType,
            boolean realTradingConfirmed) {}

    record SellCommand(long accountId, Long positionId, String stockCode,
            int quantity, BigDecimal orderPrice, String reason,
            boolean realTradingConfirmed) {}
}
