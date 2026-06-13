package seokhoon.trade.config;

public class LiveTradingDisabledException extends RuntimeException {
    public LiveTradingDisabledException(String message) {
        super(message);
    }
}
