package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.WarmUpDailyPricesAndIndicatorsUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.IndicatorWarmUpProperties;
import seokhoon.trade.domain.indicator.*;
import seokhoon.trade.domain.market.DailyPrice;

import java.time.*;
import java.util.*;

@Service
public class IndicatorWarmUpService
        implements WarmUpDailyPricesAndIndicatorsUseCase {
    private static final Logger log =
            LoggerFactory.getLogger(IndicatorWarmUpService.class);
    private static final int MA20_DAYS = 20;

    private final MarketDataPort marketDataPort;
    private final DailyPricePort dailyPricePort;
    private final IndicatorSnapshotPort indicatorSnapshotPort;
    private final IndicatorWarmUpHistoryPort historyPort;
    private final MarketCalendarPort marketCalendarPort;
    private final OperationalMetricsPort metricsPort;
    private final TechnicalIndicatorCalculator calculator;
    private final IndicatorWarmUpProperties properties;
    private final Clock clock;

    @Autowired
    public IndicatorWarmUpService(
            MarketDataPort marketDataPort,
            DailyPricePort dailyPricePort,
            IndicatorSnapshotPort indicatorSnapshotPort,
            IndicatorWarmUpHistoryPort historyPort,
            MarketCalendarPort marketCalendarPort,
            OperationalMetricsPort metricsPort,
            TechnicalIndicatorCalculator calculator,
            IndicatorWarmUpProperties properties
    ) {
        this(marketDataPort, dailyPricePort, indicatorSnapshotPort,
                historyPort, marketCalendarPort, metricsPort, calculator,
                properties, Clock.systemUTC());
    }

    IndicatorWarmUpService(
            MarketDataPort marketDataPort,
            DailyPricePort dailyPricePort,
            IndicatorSnapshotPort indicatorSnapshotPort,
            IndicatorWarmUpHistoryPort historyPort,
            MarketCalendarPort marketCalendarPort,
            OperationalMetricsPort metricsPort,
            TechnicalIndicatorCalculator calculator,
            IndicatorWarmUpProperties properties,
            Clock clock
    ) {
        this.marketDataPort = marketDataPort;
        this.dailyPricePort = dailyPricePort;
        this.indicatorSnapshotPort = indicatorSnapshotPort;
        this.historyPort = historyPort;
        this.marketCalendarPort = marketCalendarPort;
        this.metricsPort = metricsPort;
        this.calculator = calculator;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IndicatorWarmUpResult warmUpStock(
            String stockCode,
            LocalDate baseDate
    ) {
        validate(stockCode, baseDate);
        DateRange range = range(baseDate);
        if (!properties.isEnabled()) {
            return persist(result(stockCode, baseDate, range, 0, 0,
                    false, List.of("INDICATOR_WARMUP_DISABLED"),
                    IndicatorWarmUpStatus.SKIPPED), null);
        }

        try {
            List<DailyPrice> existing = load(stockCode, range);
            List<DailyPrice> fetched = List.of();
            if (existing.size() < properties.getLookbackTradingDays()) {
                fetched = marketDataPort.fetchDailyPrices(
                        stockCode, range.from(), range.to());
                if (!fetched.isEmpty()) {
                    dailyPricePort.saveAll(fetched);
                }
            }
            List<DailyPrice> prices = load(stockCode, range);
            int total = prices.size();
            boolean sufficient20 = total >= MA20_DAYS;
            boolean sufficient60 =
                    total >= properties.getMinRequiredDaysForMa60();
            boolean calculated = false;
            List<String> warnings = new ArrayList<>();
            if (sufficient60) {
                indicatorSnapshotPort.save(
                        calculator.snapshot(stockCode, prices));
                calculated = true;
            } else {
                warnings.add("INDICATOR_DATA_INSUFFICIENT");
                if (!sufficient20) {
                    warnings.add("INSUFFICIENT_FOR_MA20");
                }
                warnings.add("INSUFFICIENT_FOR_MA60");
            }
            if (fetched.isEmpty() && total == 0) {
                warnings.add("DAILY_PRICE_IMPORT_EMPTY");
            }
            IndicatorWarmUpStatus status = sufficient60
                    ? IndicatorWarmUpStatus.SUCCEEDED
                    : IndicatorWarmUpStatus.PARTIAL;
            return persist(result(stockCode, baseDate, range,
                    fetched.size(), total, calculated, warnings, status),
                    null);
        } catch (RuntimeException exception) {
            String reason = failureReason(exception);
            log.atWarn()
                    .addKeyValue("result", "failure")
                    .addKeyValue("errorType",
                            exception.getClass().getSimpleName())
                    .log("Indicator warmup failed");
            return persist(result(stockCode, baseDate, range, 0,
                    safeCount(stockCode, range), false,
                    List.of("INDICATOR_WARMUP_FAILED",
                            "INDICATOR_DATA_INSUFFICIENT"),
                    IndicatorWarmUpStatus.FAILED), reason);
        }
    }

    @Override
    public List<IndicatorWarmUpResult> warmUpStocks(
            List<String> stockCodes,
            LocalDate baseDate
    ) {
        Objects.requireNonNull(stockCodes, "stockCodes");
        Objects.requireNonNull(baseDate, "baseDate");
        return stockCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .distinct()
                .limit(properties.getMaxSymbolsPerRun())
                .map(code -> warmUpStock(code, baseDate))
                .toList();
    }

    private IndicatorWarmUpResult persist(
            IndicatorWarmUpResult result,
            String failureReason
    ) {
        historyPort.save(result, failureReason, clock.instant());
        String metric = switch (result.status()) {
            case SUCCEEDED -> "success";
            case PARTIAL -> "partial";
            case FAILED -> "failure";
            case SKIPPED -> "skipped";
        };
        metricsPort.recordIndicatorWarmUp(metric);
        metricsPort.recordIndicatorDataSufficiency(
                result.sufficientForMa60()
                        ? "sufficient" : "insufficient");
        return result;
    }

    private List<DailyPrice> load(String stockCode, DateRange range) {
        return dailyPricePort.findByStockCodeAndTradeDateBetween(
                stockCode, range.from(), range.to());
    }

    private int safeCount(String stockCode, DateRange range) {
        try {
            return load(stockCode, range).size();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private DateRange range(LocalDate baseDate) {
        LocalDate to = marketCalendarPort.previousTradingDay(baseDate);
        LocalDate from = to;
        for (int i = 1; i < properties.getLookbackTradingDays(); i++) {
            from = marketCalendarPort.previousTradingDay(from);
        }
        return new DateRange(from, to);
    }

    private IndicatorWarmUpResult result(
            String stockCode,
            LocalDate baseDate,
            DateRange range,
            int imported,
            int total,
            boolean calculated,
            List<String> warnings,
            IndicatorWarmUpStatus status
    ) {
        return new IndicatorWarmUpResult(stockCode, baseDate,
                range.from(), range.to(), imported, total, calculated,
                total >= MA20_DAYS,
                total >= properties.getMinRequiredDaysForMa60(),
                warnings, status);
    }

    private static void validate(String stockCode, LocalDate baseDate) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException(
                    "stockCode must not be blank");
        }
        Objects.requireNonNull(baseDate, "baseDate");
    }

    private static String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        String reason = message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getClass().getSimpleName() + ": " + message;
        return reason.length() <= 1000
                ? reason : reason.substring(0, 1000);
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}
