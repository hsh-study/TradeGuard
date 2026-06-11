package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.market.IntradayBar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyMarketPriceActionFeatureServiceTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);
    private static final LocalDate PREVIOUS_TRADING_DAY = LocalDate.of(2026, 6, 9);

    @Test
    void detectsPreviousHighBreakAndOpeningPriceSupport() {
        EarlyMarketPriceActionFeatureService service = service(
                List.of(previousPrice("105")),
                List.of(
                        bar("09:00", "100", "103", "99", "101"),
                        bar("09:05", "101", "106", "100", "105")
                )
        );

        var features = service.load("005930", TRADE_DATE, LocalTime.of(9, 5));

        assertThat(features.previousTradingDay()).isEqualTo(PREVIOUS_TRADING_DAY);
        assertThat(features.previousHigh()).isEqualByComparingTo("105");
        assertThat(features.openingPrice()).isEqualByComparingTo("100");
        assertThat(features.lastPrice()).isEqualByComparingTo("105");
        assertThat(features.brokePreviousHigh()).isTrue();
        assertThat(features.heldOpeningPrice()).isTrue();
        assertThat(features.dataSufficient()).isTrue();
        assertThat(features.reasons()).contains(
                "PREVIOUS_HIGH_BROKEN",
                "OPENING_PRICE_HELD"
        );
    }

    @Test
    void detectsOpeningPriceLoss() {
        var features = service(
                List.of(previousPrice("110")),
                List.of(
                        bar("09:00", "100", "102", "98", "99"),
                        bar("09:20", "99", "100", "96", "97")
                )
        ).load("005930", TRADE_DATE, LocalTime.of(9, 20));

        assertThat(features.heldOpeningPrice()).isFalse();
        assertThat(features.pullbackRecovered()).isFalse();
        assertThat(features.reasons()).contains("OPENING_PRICE_LOST");
    }

    @Test
    void detectsPullbackRecovery() {
        var features = service(
                List.of(previousPrice("101")),
                List.of(
                        bar("09:00", "100", "102", "98", "99"),
                        bar("09:20", "99", "103", "99", "102")
                )
        ).load("005930", TRADE_DATE, LocalTime.of(9, 20));

        assertThat(features.brokePreviousHigh()).isTrue();
        assertThat(features.heldOpeningPrice()).isTrue();
        assertThat(features.pullbackRecovered()).isTrue();
        assertThat(features.reasons()).contains("PULLBACK_RECOVERED");
    }

    @Test
    void returnsInsufficientWhenPreviousPriceOrBarsAreMissing() {
        var features = service(List.of(), List.of())
                .load("005930", TRADE_DATE, LocalTime.of(9, 20));

        assertThat(features.dataSufficient()).isFalse();
        assertThat(features.brokePreviousHigh()).isNull();
        assertThat(features.reasons()).contains(
                "PREVIOUS_HIGH_UNAVAILABLE",
                "INTRADAY_BARS_UNAVAILABLE"
        );
    }

    private static EarlyMarketPriceActionFeatureService service(
            List<DailyPrice> prices,
            List<IntradayBar> bars
    ) {
        DailyPricePort dailyPricePort = new DailyPricePort() {
            @Override
            public List<DailyPrice> saveAll(List<DailyPrice> dailyPrices) {
                return dailyPrices;
            }

            @Override
            public List<DailyPrice> findByStockCodeAndTradeDateBetween(
                    String stockCode,
                    LocalDate from,
                    LocalDate to
            ) {
                return prices;
            }
        };
        return new EarlyMarketPriceActionFeatureService(
                dailyPricePort,
                (stockCode, tradeDate, from, to, interval) -> bars,
                date -> !date.getDayOfWeek().name().startsWith("S"),
                OperationalMetricsPort.noop()
        );
    }

    private static DailyPrice previousPrice(String high) {
        return new DailyPrice(
                "005930",
                PREVIOUS_TRADING_DAY,
                new BigDecimal("100"),
                new BigDecimal(high),
                new BigDecimal("95"),
                new BigDecimal("102"),
                100,
                BigDecimal.valueOf(10_000)
        );
    }

    private static IntradayBar bar(
            String time,
            String open,
            String high,
            String low,
            String close
    ) {
        return new IntradayBar(
                "005930",
                TRADE_DATE,
                LocalTime.parse(time),
                new BigDecimal(open),
                new BigDecimal(high),
                new BigDecimal(low),
                new BigDecimal(close),
                100,
                BigDecimal.valueOf(10_000),
                new BigDecimal("99")
        );
    }
}
