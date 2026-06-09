package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.strategy.TradingSignal;

import java.math.BigDecimal;

public interface RequestMockOrderUseCase {
    MockOrderResult request(TradingSignal signal, int quantity, BigDecimal limitPrice);

    default MockOrderResult request(
            TradingSignal signal,
            Long signalId,
            int quantity,
            BigDecimal limitPrice
    ) {
        return request(signal, quantity, limitPrice);
    }
}
