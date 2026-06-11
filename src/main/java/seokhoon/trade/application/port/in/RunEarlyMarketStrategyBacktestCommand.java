package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public record RunEarlyMarketStrategyBacktestCommand(
        String experimentName,
        LocalDate from,
        LocalDate to,
        EarlyMarketStrategyParameterOverrides parameterOverrides
) {
}
