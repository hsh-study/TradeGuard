package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.LoadMarketCalendarUseCase;
import seokhoon.trade.application.port.in.MarketCalendarView;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/market-calendar/trading-days")
public class MarketCalendarController {
    private final LoadMarketCalendarUseCase loadMarketCalendar;

    public MarketCalendarController(LoadMarketCalendarUseCase loadMarketCalendar) {
        this.loadMarketCalendar = loadMarketCalendar;
    }

    @GetMapping
    TradingDayResponse find(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return TradingDayResponse.from(loadMarketCalendar.load(date));
    }

    public record TradingDayResponse(
            LocalDate date,
            boolean tradingDay,
            LocalDate previousTradingDay,
            LocalDate nextTradingDay
    ) {
        static TradingDayResponse from(MarketCalendarView view) {
            return new TradingDayResponse(
                    view.date(),
                    view.tradingDay(),
                    view.previousTradingDay(),
                    view.nextTradingDay()
            );
        }
    }
}
