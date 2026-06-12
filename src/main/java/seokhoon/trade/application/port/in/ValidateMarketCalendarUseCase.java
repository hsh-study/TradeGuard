package seokhoon.trade.application.port.in;

public interface ValidateMarketCalendarUseCase {
    MarketCalendarValidationResult validate(int year);
}
