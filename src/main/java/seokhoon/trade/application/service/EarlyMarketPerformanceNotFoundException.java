package seokhoon.trade.application.service;

public class EarlyMarketPerformanceNotFoundException extends RuntimeException {
    public EarlyMarketPerformanceNotFoundException(long signalId) {
        super("Early market candidate performance not found: " + signalId);
    }
}
