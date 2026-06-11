package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.LoadMarketCalendarDaysUseCase;
import seokhoon.trade.application.port.in.LoadMarketCalendarUseCase;
import seokhoon.trade.application.port.in.MarketCalendarSyncResult;
import seokhoon.trade.application.port.in.MarketCalendarView;
import seokhoon.trade.application.port.in.SyncMarketCalendarUseCase;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/market-calendar")
public class MarketCalendarController {
    private final LoadMarketCalendarUseCase loadMarketCalendar;
    private final LoadMarketCalendarDaysUseCase loadMarketCalendarDays;
    private final SyncMarketCalendarUseCase syncMarketCalendar;

    public MarketCalendarController(
            LoadMarketCalendarUseCase loadMarketCalendar,
            LoadMarketCalendarDaysUseCase loadMarketCalendarDays,
            SyncMarketCalendarUseCase syncMarketCalendar
    ) {
        this.loadMarketCalendar = loadMarketCalendar;
        this.loadMarketCalendarDays = loadMarketCalendarDays;
        this.syncMarketCalendar = syncMarketCalendar;
    }

    @GetMapping("/trading-days")
    TradingDayResponse find(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return TradingDayResponse.from(loadMarketCalendar.load(date));
    }

    @PostMapping("/sync")
    SyncResponse sync(@RequestParam int year) {
        return SyncResponse.from(syncMarketCalendar.syncYear(year));
    }

    @GetMapping("/days")
    List<CalendarDayResponse> findDays(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return loadMarketCalendarDays.load(from, to)
                .stream()
                .map(CalendarDayResponse::from)
                .toList();
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

    public record SyncResponse(
            int syncedCount,
            int tradingDayCount,
            int holidayCount,
            MarketCalendarSource source,
            List<String> warnings
    ) {
        static SyncResponse from(MarketCalendarSyncResult result) {
            return new SyncResponse(
                    result.syncedCount(),
                    result.tradingDayCount(),
                    result.holidayCount(),
                    result.source(),
                    result.warnings()
            );
        }
    }

    public record CalendarDayResponse(
            String market,
            LocalDate date,
            boolean tradingDay,
            String holidayName,
            MarketCalendarSource source
    ) {
        static CalendarDayResponse from(MarketCalendarDay day) {
            return new CalendarDayResponse(
                    day.market(),
                    day.date(),
                    day.tradingDay(),
                    day.holidayName(),
                    day.source()
            );
        }
    }
}
