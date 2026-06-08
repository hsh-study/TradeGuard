package seokhoon.trade.application.port.in;

import java.math.BigDecimal;

public record SignalMockOrderCommand(int quantity, BigDecimal limitPrice) {
}
