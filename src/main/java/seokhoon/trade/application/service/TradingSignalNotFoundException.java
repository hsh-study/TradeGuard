package seokhoon.trade.application.service;

public class TradingSignalNotFoundException extends RuntimeException {
    public TradingSignalNotFoundException() {
        super("Trading signal not found");
    }
}
