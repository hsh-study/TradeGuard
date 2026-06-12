package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.LoadMarketCalendarAuditsUseCase;
import seokhoon.trade.application.port.in.MarketCalendarDayOverrideResult;
import seokhoon.trade.application.port.in.MarketCalendarValidationResult;
import seokhoon.trade.application.port.in.OverrideMarketCalendarDayCommand;
import seokhoon.trade.application.port.in.OverrideMarketCalendarDayUseCase;
import seokhoon.trade.application.port.in.ValidateMarketCalendarUseCase;
import seokhoon.trade.application.port.out.MarketCalendarDayAuditPort;
import seokhoon.trade.application.port.out.MarketCalendarDayPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarDayAudit;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MarketCalendarManagementService
        implements OverrideMarketCalendarDayUseCase,
        ValidateMarketCalendarUseCase,
        LoadMarketCalendarAuditsUseCase {
    private static final Logger log =
            LoggerFactory.getLogger(MarketCalendarManagementService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final String DEFAULT_ACTOR = "MANUAL_API";

    private final MarketCalendarDayPort calendarDayPort;
    private final MarketCalendarDayAuditPort auditPort;
    private final OperationalMetricsPort metricsPort;
    private final Clock clock;

    @Autowired
    public MarketCalendarManagementService(
            MarketCalendarDayPort calendarDayPort,
            MarketCalendarDayAuditPort auditPort,
            OperationalMetricsPort metricsPort
    ) {
        this(
                calendarDayPort,
                auditPort,
                metricsPort,
                Clock.system(SEOUL)
        );
    }

    MarketCalendarManagementService(
            MarketCalendarDayPort calendarDayPort,
            MarketCalendarDayAuditPort auditPort,
            OperationalMetricsPort metricsPort,
            Clock clock
    ) {
        this.calendarDayPort = calendarDayPort;
        this.auditPort = auditPort;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MarketCalendarDayOverrideResult override(
            OverrideMarketCalendarDayCommand command
    ) {
        try {
            validateCommand(command);
            String market = normalizeMarket(command.market());
            MarketCalendarDay before = calendarDayPort.findByDate(command.date())
                    .filter(day -> day.market().equals(market))
                    .orElse(null);
            MarketCalendarDay override = new MarketCalendarDay(
                    market,
                    command.date(),
                    command.tradingDay(),
                    command.tradingDay() ? null : command.holidayName(),
                    MarketCalendarSource.MANUAL_OVERRIDE
            );
            MarketCalendarDay saved = calendarDayPort.save(override);
            MarketCalendarDayAudit audit = auditPort.save(
                    new MarketCalendarDayAudit(
                            null,
                            market,
                            command.date(),
                            before == null ? null : before.tradingDay(),
                            saved.tradingDay(),
                            before == null ? null : before.holidayName(),
                            saved.holidayName(),
                            command.reason(),
                            normalizeActor(command.actor()),
                            Instant.now(clock)
                    )
            );
            metricsPort.recordMarketCalendarOverride("success");
            log.atInfo()
                    .addKeyValue("market", market)
                    .addKeyValue("tradeDate", command.date())
                    .addKeyValue("actor", audit.actor())
                    .log("Market calendar day manually overridden");
            return new MarketCalendarDayOverrideResult(saved, audit);
        } catch (RuntimeException exception) {
            metricsPort.recordMarketCalendarOverride("failure");
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MarketCalendarValidationResult validate(int year) {
        try {
            if (year < 2000 || year > 2100) {
                throw new IllegalArgumentException(
                        "year must be between 2000 and 2100"
                );
            }
            LocalDate from = LocalDate.of(year, 1, 1);
            LocalDate to = LocalDate.of(year, 12, 31);
            List<MarketCalendarDay> days = calendarDayPort.findBetween(from, to);
            Map<LocalDate, MarketCalendarDay> byDate = new HashMap<>();
            days.forEach(day -> byDate.put(day.date(), day));

            List<LocalDate> missingDays = new ArrayList<>();
            List<LocalDate> weekendTradingDays = new ArrayList<>();
            List<LocalDate> weekdayHolidays = new ArrayList<>();
            int tradingDayCount = 0;
            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                MarketCalendarDay day = byDate.get(date);
                if (day == null) {
                    missingDays.add(date);
                    continue;
                }
                if (day.tradingDay()) {
                    tradingDayCount++;
                    if (isWeekend(date)) {
                        weekendTradingDays.add(date);
                    }
                } else if (!isWeekend(date)) {
                    weekdayHolidays.add(date);
                }
            }

            Map<String, Integer> sourceDistribution = sourceDistribution(days);
            List<String> warnings = warnings(
                    missingDays,
                    weekendTradingDays,
                    weekdayHolidays,
                    hasTradingDayWithin30Days()
            );
            MarketCalendarValidationResult result =
                    new MarketCalendarValidationResult(
                            year,
                            days.size(),
                            tradingDayCount,
                            days.size() - tradingDayCount,
                            weekendTradingDays.size(),
                            weekdayHolidays.size(),
                            List.copyOf(weekendTradingDays),
                            List.copyOf(weekdayHolidays),
                            List.copyOf(missingDays),
                            Collections.unmodifiableMap(sourceDistribution),
                            List.copyOf(warnings)
                    );
            metricsPort.recordMarketCalendarValidation("success");
            return result;
        } catch (RuntimeException exception) {
            metricsPort.recordMarketCalendarValidation("failure");
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketCalendarDayAudit> load(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be on or before to");
        }
        return auditPort.findBetween(from, to);
    }

    private boolean hasTradingDayWithin30Days() {
        LocalDate today = LocalDate.now(clock);
        return calendarDayPort.findBetween(today, today.plusDays(30))
                .stream()
                .anyMatch(MarketCalendarDay::tradingDay);
    }

    private static Map<String, Integer> sourceDistribution(
            List<MarketCalendarDay> days
    ) {
        Map<MarketCalendarSource, Integer> counts =
                new EnumMap<>(MarketCalendarSource.class);
        for (MarketCalendarSource source : MarketCalendarSource.values()) {
            counts.put(source, 0);
        }
        days.forEach(day -> counts.compute(
                day.source(),
                (source, count) -> count == null ? 1 : count + 1
        ));
        Map<String, Integer> result = new java.util.LinkedHashMap<>();
        for (MarketCalendarSource source : MarketCalendarSource.values()) {
            result.put(source.name(), counts.get(source));
        }
        return result;
    }

    private static List<String> warnings(
            List<LocalDate> missingDays,
            List<LocalDate> weekendTradingDays,
            List<LocalDate> weekdayHolidays,
            boolean hasTradingDayWithin30Days
    ) {
        List<String> warnings = new ArrayList<>();
        if (!missingDays.isEmpty()) {
            warnings.add("MISSING_DAYS: " + missingDays.size());
        }
        if (!weekendTradingDays.isEmpty()) {
            warnings.add(
                    "WEEKEND_TRADING_DAYS: " + weekendTradingDays.size()
            );
        }
        if (!weekdayHolidays.isEmpty()) {
            warnings.add("WEEKDAY_HOLIDAYS: " + weekdayHolidays.size());
        }
        if (!hasTradingDayWithin30Days) {
            warnings.add("NO_TRADING_DAY_WITHIN_NEXT_30_DAYS");
        }
        return warnings;
    }

    private static void validateCommand(OverrideMarketCalendarDayCommand command) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(command.date(), "date");
        if (command.reason() == null || command.reason().isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        String market = normalizeMarket(command.market());
        if (!MarketCalendarDay.KRX_STOCK.equals(market)) {
            throw new IllegalArgumentException("market must be KRX_STOCK");
        }
    }

    private static String normalizeMarket(String market) {
        return market == null || market.isBlank()
                ? MarketCalendarDay.KRX_STOCK
                : market.trim();
    }

    private static String normalizeActor(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
