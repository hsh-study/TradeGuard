package seokhoon.trade.application.service;

public class ReplayBacktestNotFoundException extends RuntimeException {
    public ReplayBacktestNotFoundException(long runId) {
        super("Replay backtest run not found: " + runId);
    }
}
