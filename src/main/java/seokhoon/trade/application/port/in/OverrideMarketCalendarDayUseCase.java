package seokhoon.trade.application.port.in;

public interface OverrideMarketCalendarDayUseCase {
    MarketCalendarDayOverrideResult override(OverrideMarketCalendarDayCommand command);
}
