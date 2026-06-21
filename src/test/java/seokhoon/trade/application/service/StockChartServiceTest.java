package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.application.port.out.IntradayBarPort;
import seokhoon.trade.application.port.in.GetStockChartUseCase.ChartInterval;
import seokhoon.trade.domain.indicator.TechnicalIndicatorCalculator;
import seokhoon.trade.domain.market.IntradayBar;
import seokhoon.trade.domain.market.DailyPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StockChartServiceTest {
    @Test
    void returnsOrderedOhlcvAndCalculatedIndicatorSeries() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        List<DailyPrice> prices = new ArrayList<>();
        for (int i = 69; i >= 0; i--) {
            BigDecimal close = BigDecimal.valueOf(100 + i);
            prices.add(new DailyPrice("005930", start.plusDays(i), close,
                    close.add(BigDecimal.ONE), close.subtract(BigDecimal.ONE), close,
                    1_000L + i, close.multiply(BigDecimal.valueOf(1_000L + i))));
        }
        DailyPricePort port = new DailyPricePort() {
            @Override public List<DailyPrice> saveAll(List<DailyPrice> values) { return values; }
            @Override public List<DailyPrice> findByStockCodeAndTradeDateBetween(
                    String code, LocalDate from, LocalDate to) { return prices; }
        };

        var result = new StockChartService(port, new TechnicalIndicatorCalculator())
                .getChart("005930", start, start.plusDays(100));

        assertThat(result.dataPointCount()).isEqualTo(70);
        assertThat(result.points()).isSortedAccordingTo(
                java.util.Comparator.comparing(p -> p.tradeDate()));
        assertThat(result.points().get(4).ma5()).isNotNull();
        assertThat(result.points().get(19).bollingerUpper()).isNotNull();
        assertThat(result.points().get(34).macd()).isNotNull();
        assertThat(result.points().get(59).ma60()).isNotNull();
        assertThat(result.points().getFirst().ma5()).isNull();
    }

    @Test
    void aggregatesOneMinuteBarsIntoRequestedThreeMinuteCandles() {
        DailyPricePort prices = mock(DailyPricePort.class);
        IntradayBarPort bars = (code, date, from, to, interval) -> {
            List<IntradayBar> result = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                BigDecimal price = BigDecimal.valueOf(100 + i);
                result.add(new IntradayBar(code, date, LocalTime.of(9, i), price,
                        price.add(BigDecimal.ONE), price.subtract(BigDecimal.ONE), price,
                        10, price.multiply(BigDecimal.TEN), price));
            }
            return result;
        };
        Clock clock = Clock.fixed(Instant.parse("2026-06-22T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        var service = new StockChartService(prices, bars,
                new TechnicalIndicatorCalculator(), clock);

        var result = service.getChart("005930", LocalDate.of(2026, 6, 22),
                LocalDate.of(2026, 6, 22), ChartInterval.MINUTE_3);

        assertThat(result.points()).hasSize(2);
        assertThat(result.points().getFirst().barTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.points().getFirst().open()).isEqualByComparingTo("100");
        assertThat(result.points().getFirst().close()).isEqualByComparingTo("102");
        assertThat(result.points().getFirst().volume()).isEqualTo(30);
    }
}
