package seokhoon.trade.application.service;

import java.time.LocalDate;

public class EarlyMarketStrategyExperimentNoDataException
        extends RuntimeException {
    public EarlyMarketStrategyExperimentNoDataException(
            LocalDate from,
            LocalDate to
    ) {
        super("No early market strategy candidates found from "
                + from + " to " + to);
    }
}
