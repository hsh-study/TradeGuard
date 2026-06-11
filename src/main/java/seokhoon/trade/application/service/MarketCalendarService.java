package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.LoadMarketCalendarUseCase;
import seokhoon.trade.application.port.in.MarketCalendarView;
import seokhoon.trade.application.port.out.MarketCalendarPort;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class MarketCalendarService implements LoadMarketCalendarUseCase {
    private final MarketCalendarPort marketCalendarPort;

    public MarketCalendarService(MarketCalendarPort marketCalendarPort) {
        this.marketCalendarPort = marketCalendarPort;
    }

    @Override
    public MarketCalendarView load(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return new MarketCalendarView(
                date,
                marketCalendarPort.isTradingDay(date),
                marketCalendarPort.previousTradingDay(date),
                marketCalendarPort.nextTradingDay(date)
        );
    }
}
