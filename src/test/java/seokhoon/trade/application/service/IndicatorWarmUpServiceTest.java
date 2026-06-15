package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.IndicatorWarmUpProperties;
import seokhoon.trade.domain.indicator.*;
import seokhoon.trade.domain.market.DailyPrice;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class IndicatorWarmUpServiceTest {
    private static final LocalDate BASE_DATE =
            LocalDate.of(2026, 6, 15);

    @Test
    void importsOneHundredTwentyTradingDaysAndCreatesSnapshot() {
        Fixture fixture = fixture(120, false);

        IndicatorWarmUpResult result =
                fixture.service.warmUpStock("005930", BASE_DATE);

        assertThat(result.status())
                .isEqualTo(IndicatorWarmUpStatus.SUCCEEDED);
        assertThat(result.importedDailyPriceCount()).isEqualTo(120);
        assertThat(result.totalDailyPriceCount()).isEqualTo(120);
        assertThat(result.sufficientForMa20()).isTrue();
        assertThat(result.sufficientForMa60()).isTrue();
        assertThat(result.indicatorCalculated()).isTrue();
        assertThat(fixture.snapshots).hasSize(1);
        assertThat(fixture.histories).hasSize(1);
    }

    @Test
    void marksMa60InsufficientWhenFewerThanSixtyPricesExist() {
        Fixture fixture = fixture(59, false);

        IndicatorWarmUpResult result =
                fixture.service.warmUpStock("005930", BASE_DATE);

        assertThat(result.status())
                .isEqualTo(IndicatorWarmUpStatus.PARTIAL);
        assertThat(result.sufficientForMa20()).isTrue();
        assertThat(result.sufficientForMa60()).isFalse();
        assertThat(result.indicatorCalculated()).isFalse();
        assertThat(result.warnings())
                .contains("INDICATOR_DATA_INSUFFICIENT",
                        "INSUFFICIENT_FOR_MA60");
    }

    @Test
    void recordsFailureWithoutThrowingWhenKisImportFails() {
        Fixture fixture = fixture(0, true);

        IndicatorWarmUpResult result =
                fixture.service.warmUpStock("005930", BASE_DATE);

        assertThat(result.status())
                .isEqualTo(IndicatorWarmUpStatus.FAILED);
        assertThat(result.warnings())
                .contains("INDICATOR_WARMUP_FAILED");
        assertThat(fixture.histories).singleElement()
                .satisfies(history -> assertThat(
                        history.failureReason()).contains("KIS unavailable"));
    }

    private static Fixture fixture(int fetchedCount, boolean fail) {
        IndicatorWarmUpProperties properties =
                new IndicatorWarmUpProperties();
        List<DailyPrice> stored = new ArrayList<>();
        List<IndicatorSnapshot> snapshots = new ArrayList<>();
        List<IndicatorWarmUpHistory> histories = new ArrayList<>();
        MarketCalendarPort calendar = date ->
                date.getDayOfWeek() != DayOfWeek.SATURDAY
                        && date.getDayOfWeek() != DayOfWeek.SUNDAY;
        MarketDataPort marketData = (stockCode, from, to) -> {
            if (fail) {
                throw new IllegalStateException("KIS unavailable");
            }
            return prices(stockCode, to, fetchedCount, calendar);
        };
        DailyPricePort dailyPrices = new DailyPricePort() {
            @Override
            public List<DailyPrice> saveAll(List<DailyPrice> values) {
                values.forEach(value -> {
                    stored.removeIf(existing ->
                            existing.stockCode().equals(value.stockCode())
                                    && existing.tradeDate().equals(
                                    value.tradeDate()));
                    stored.add(value);
                });
                return values;
            }

            @Override
            public List<DailyPrice> findByStockCodeAndTradeDateBetween(
                    String stockCode, LocalDate from, LocalDate to) {
                return stored.stream()
                        .filter(value -> value.stockCode().equals(stockCode))
                        .filter(value -> !value.tradeDate().isBefore(from))
                        .filter(value -> !value.tradeDate().isAfter(to))
                        .sorted(Comparator.comparing(DailyPrice::tradeDate))
                        .toList();
            }
        };
        IndicatorSnapshotPort snapshotPort =
                new IndicatorSnapshotPort() {
                    @Override
                    public IndicatorSnapshot save(
                            IndicatorSnapshot snapshot) {
                        snapshots.add(snapshot);
                        return snapshot;
                    }

                    @Override
                    public List<IndicatorSnapshot>
                    findByStockCodeAndTradeDateBetween(
                            String stockCode, LocalDate from,
                            LocalDate to) {
                        return snapshots;
                    }
                };
        IndicatorWarmUpHistoryPort historyPort =
                new IndicatorWarmUpHistoryPort() {
                    @Override
                    public IndicatorWarmUpHistory save(
                            IndicatorWarmUpResult result,
                            String failureReason, Instant createdAt) {
                        IndicatorWarmUpHistory history =
                                new IndicatorWarmUpHistory(
                                        1L, result.stockCode(),
                                        result.baseDate(), result.status(),
                                        result.importedDailyPriceCount(),
                                        result.totalDailyPriceCount(),
                                        result.sufficientForMa20(),
                                        result.sufficientForMa60(),
                                        failureReason, createdAt);
                        histories.add(history);
                        return history;
                    }

                    @Override
                    public List<IndicatorWarmUpHistory> findByStockCode(
                            String stockCode) {
                        return histories;
                    }
                };
        IndicatorWarmUpService service = new IndicatorWarmUpService(
                marketData, dailyPrices, snapshotPort, historyPort,
                calendar, OperationalMetricsPort.noop(),
                new TechnicalIndicatorCalculator(), properties,
                Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"),
                        ZoneOffset.UTC));
        return new Fixture(service, snapshots, histories);
    }

    private static List<DailyPrice> prices(
            String stockCode,
            LocalDate to,
            int count,
            MarketCalendarPort calendar
    ) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = to;
        while (dates.size() < count) {
            if (calendar.isTradingDay(date)) {
                dates.add(date);
            }
            date = date.minusDays(1);
        }
        Collections.reverse(dates);
        List<DailyPrice> prices = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            BigDecimal close = BigDecimal.valueOf(70_000L + i * 100L);
            prices.add(new DailyPrice(stockCode, dates.get(i), close,
                    close.add(BigDecimal.valueOf(500)),
                    close.subtract(BigDecimal.valueOf(500)), close,
                    1_000_000L,
                    close.multiply(BigDecimal.valueOf(1_000_000L))));
        }
        return prices;
    }

    private record Fixture(
            IndicatorWarmUpService service,
            List<IndicatorSnapshot> snapshots,
            List<IndicatorWarmUpHistory> histories
    ) {
    }
}
