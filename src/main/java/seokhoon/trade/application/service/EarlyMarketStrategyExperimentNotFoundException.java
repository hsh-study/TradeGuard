package seokhoon.trade.application.service;

public class EarlyMarketStrategyExperimentNotFoundException
        extends RuntimeException {
    public EarlyMarketStrategyExperimentNotFoundException(long id) {
        super("Early market strategy experiment not found: " + id);
    }
}
