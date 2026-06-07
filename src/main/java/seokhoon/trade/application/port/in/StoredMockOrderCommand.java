package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StoredMockOrderCommand(
        String strategyName,
        String stockCode,
        LocalDate signalDate,
        SignalType signalType,
        int quantity,
        BigDecimal limitPrice
) {
}
