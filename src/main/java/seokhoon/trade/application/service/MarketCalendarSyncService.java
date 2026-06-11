package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.LoadMarketCalendarDaysUseCase;
import seokhoon.trade.application.port.in.MarketCalendarSyncResult;
import seokhoon.trade.application.port.in.SyncMarketCalendarUseCase;
import seokhoon.trade.application.port.out.MarketCalendarDayPort;
import seokhoon.trade.application.port.out.MarketCalendarSyncProvider;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class MarketCalendarSyncService
        implements SyncMarketCalendarUseCase, LoadMarketCalendarDaysUseCase {
    private static final Logger log =
            LoggerFactory.getLogger(MarketCalendarSyncService.class);

    private final MarketCalendarDayPort calendarDayPort;
    private final MarketCalendarSyncProvider krxProvider;
    private final MarketCalendarSyncProvider fallbackProvider;
    private final OperationalMetricsPort metricsPort;

    @Autowired
    public MarketCalendarSyncService(
            MarketCalendarDayPort calendarDayPort,
            @Qualifier("krxMarketCalendarSyncProvider")
            MarketCalendarSyncProvider krxProvider,
            @Qualifier("fallbackGeneratedMarketCalendarSyncProvider")
            MarketCalendarSyncProvider fallbackProvider,
            OperationalMetricsPort metricsPort
    ) {
        this.calendarDayPort = calendarDayPort;
        this.krxProvider = krxProvider;
        this.fallbackProvider = fallbackProvider;
        this.metricsPort = metricsPort;
    }

    MarketCalendarSyncService(
            MarketCalendarDayPort calendarDayPort,
            MarketCalendarSyncProvider krxProvider,
            MarketCalendarSyncProvider fallbackProvider,
            OperationalMetricsPort metricsPort,
            boolean testConstructor
    ) {
        this.calendarDayPort = calendarDayPort;
        this.krxProvider = krxProvider;
        this.fallbackProvider = fallbackProvider;
        this.metricsPort = metricsPort;
    }

    @Override
    public MarketCalendarSyncResult syncYear(int year) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("year must be between 2000 and 2100");
        }
        List<String> warnings = new ArrayList<>();
        List<MarketCalendarDay> days;
        MarketCalendarSource source;
        String metricResult;
        try {
            days = krxProvider.fetchYear(year);
            source = MarketCalendarSource.KRX_OFFICIAL;
            metricResult = "success";
        } catch (RuntimeException exception) {
            warnings.add("KRX_OFFICIAL_UNAVAILABLE: " + safeMessage(exception));
            log.atWarn()
                    .addKeyValue("year", year)
                    .addKeyValue("fallbackSource", MarketCalendarSource.FALLBACK_GENERATED)
                    .addKeyValue("failureType", exception.getClass().getSimpleName())
                    .log("KRX market calendar sync failed; using generated fallback");
            try {
                days = fallbackProvider.fetchYear(year);
                source = MarketCalendarSource.FALLBACK_GENERATED;
                metricResult = "fallback";
            } catch (RuntimeException fallbackException) {
                metricsPort.recordMarketCalendarSync("failure", year);
                throw fallbackException;
            }
        }
        try {
            calendarDayPort.upsertAll(days);
            metricsPort.recordMarketCalendarSync(metricResult, year);
        } catch (RuntimeException exception) {
            metricsPort.recordMarketCalendarSync("failure", year);
            throw exception;
        }
        return result(days, source, warnings);
    }

    @Override
    public MarketCalendarSyncResult syncRange(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<MarketCalendarDay> synced = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        MarketCalendarSource source = MarketCalendarSource.KRX_OFFICIAL;
        for (int year = from.getYear(); year <= to.getYear(); year++) {
            MarketCalendarSyncResult result = syncYear(year);
            warnings.addAll(result.warnings());
            if (result.source() == MarketCalendarSource.FALLBACK_GENERATED) {
                source = MarketCalendarSource.FALLBACK_GENERATED;
            }
            synced.addAll(calendarDayPort.findBetween(
                    year == from.getYear() ? from : LocalDate.of(year, 1, 1),
                    year == to.getYear() ? to : LocalDate.of(year, 12, 31)
            ));
        }
        return result(synced, source, warnings);
    }

    @Override
    public List<MarketCalendarDay> load(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return calendarDayPort.findBetween(from, to);
    }

    private static MarketCalendarSyncResult result(
            List<MarketCalendarDay> days,
            MarketCalendarSource source,
            List<String> warnings
    ) {
        int tradingDayCount = (int) days.stream()
                .filter(MarketCalendarDay::tradingDay)
                .count();
        return new MarketCalendarSyncResult(
                days.size(),
                tradingDayCount,
                days.size() - tradingDayCount,
                source,
                List.copyOf(warnings)
        );
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be on or before to");
        }
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getClass().getSimpleName();
    }
}
