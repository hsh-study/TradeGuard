package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.strategy.TradingSignal;

import java.math.BigDecimal;

public interface RequestMockOrderUseCase {
    MockOrderResult request(TradingSignal signal, int quantity, BigDecimal limitPrice);
}
