package seokhoon.trade.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.LoadMarketCalendarDaysUseCase;
import seokhoon.trade.application.port.in.LoadMarketCalendarAuditsUseCase;
import seokhoon.trade.application.port.in.LoadMarketCalendarUseCase;
import seokhoon.trade.application.port.in.MarketCalendarDayOverrideResult;
import seokhoon.trade.application.port.in.MarketCalendarSyncResult;
import seokhoon.trade.application.port.in.MarketCalendarValidationResult;
import seokhoon.trade.application.port.in.MarketCalendarView;
import seokhoon.trade.application.port.in.OverrideMarketCalendarDayCommand;
import seokhoon.trade.application.port.in.OverrideMarketCalendarDayUseCase;
import seokhoon.trade.application.port.in.SyncMarketCalendarUseCase;
import seokhoon.trade.application.port.in.ValidateMarketCalendarUseCase;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarDayAudit;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/market-calendar")
public class MarketCalendarController {
    private final LoadMarketCalendarUseCase loadMarketCalendar;
    private final LoadMarketCalendarDaysUseCase loadMarketCalendarDays;
    private final SyncMarketCalendarUseCase syncMarketCalendar;
    private final OverrideMarketCalendarDayUseCase overrideMarketCalendarDay;
    private final ValidateMarketCalendarUseCase validateMarketCalendar;
    private final LoadMarketCalendarAuditsUseCase loadMarketCalendarAudits;

    public MarketCalendarController(
            LoadMarketCalendarUseCase loadMarketCalendar,
            LoadMarketCalendarDaysUseCase loadMarketCalendarDays,
            SyncMarketCalendarUseCase syncMarketCalendar,
            OverrideMarketCalendarDayUseCase overrideMarketCalendarDay,
            ValidateMarketCalendarUseCase validateMarketCalendar,
            LoadMarketCalendarAuditsUseCase loadMarketCalendarAudits
    ) {
        this.loadMarketCalendar = loadMarketCalendar;
        this.loadMarketCalendarDays = loadMarketCalendarDays;
        this.syncMarketCalendar = syncMarketCalendar;
        this.overrideMarketCalendarDay = overrideMarketCalendarDay;
        this.validateMarketCalendar = validateMarketCalendar;
        this.loadMarketCalendarAudits = loadMarketCalendarAudits;
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

    @PatchMapping("/days/{date}")
    OverrideResponse override(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody OverrideRequest request
    ) {
        return OverrideResponse.from(overrideMarketCalendarDay.override(
                new OverrideMarketCalendarDayCommand(
                        request.market(),
                        date,
                        request.tradingDay(),
                        request.holidayName(),
                        request.reason(),
                        request.actor()
                )
        ));
    }

    @GetMapping("/validation")
    MarketCalendarValidationResult validate(@RequestParam int year) {
        return validateMarketCalendar.validate(year);
    }

    @GetMapping("/audits")
    List<AuditResponse> findAudits(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return loadMarketCalendarAudits.load(from, to)
                .stream()
                .map(AuditResponse::from)
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

    public record OverrideRequest(
            @Size(max = 30) String market,
            @NotNull Boolean tradingDay,
            @Size(max = 200) String holidayName,
            @NotBlank @Size(max = 500) String reason,
            @Size(max = 100) String actor
    ) {
    }

    public record OverrideResponse(
            CalendarDayResponse day,
            AuditResponse audit
    ) {
        static OverrideResponse from(MarketCalendarDayOverrideResult result) {
            return new OverrideResponse(
                    CalendarDayResponse.from(result.day()),
                    AuditResponse.from(result.audit())
            );
        }
    }

    public record AuditResponse(
            Long id,
            String market,
            LocalDate date,
            Boolean beforeTradingDay,
            boolean afterTradingDay,
            String beforeHolidayName,
            String afterHolidayName,
            String reason,
            String actor,
            java.time.Instant createdAt
    ) {
        static AuditResponse from(MarketCalendarDayAudit audit) {
            return new AuditResponse(
                    audit.id(),
                    audit.market(),
                    audit.date(),
                    audit.beforeTradingDay(),
                    audit.afterTradingDay(),
                    audit.beforeHolidayName(),
                    audit.afterHolidayName(),
                    audit.reason(),
                    audit.actor(),
                    audit.createdAt()
            );
        }
    }
}
