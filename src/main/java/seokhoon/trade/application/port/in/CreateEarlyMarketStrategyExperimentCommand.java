package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public record CreateEarlyMarketStrategyExperimentCommand(
        String experimentName,
        LocalDate from,
        LocalDate to
) {
}
