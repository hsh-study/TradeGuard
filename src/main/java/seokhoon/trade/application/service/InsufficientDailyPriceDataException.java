package seokhoon.trade.application.service;

public class InsufficientDailyPriceDataException extends IllegalStateException {
    public InsufficientDailyPriceDataException(int minimumPriceCount) {
        super("At least " + minimumPriceCount + " daily prices are required for analysis");
    }
}
