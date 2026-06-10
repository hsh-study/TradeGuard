package seokhoon.trade.adapter.marketdata;

import org.junit.jupiter.api.Test;
import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.IntradayBar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FakeIntradayBarAdapterTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);

    @Test
    void returnsInjectedBarsWithinRangeInTimeOrder() {
        FakeIntradayBarAdapter adapter = new FakeIntradayBarAdapter(List.of(
                bar("09:10"),
                bar("09:00"),
                bar("09:05"),
                bar("09:31")
        ));

        assertThat(adapter.findBars(
                "005930",
                TRADE_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                BarInterval.FIVE_MINUTES
        )).extracting(IntradayBar::barTime)
                .containsExactly(
                        LocalTime.of(9, 0),
                        LocalTime.of(9, 5),
                        LocalTime.of(9, 10)
                );
    }

    @Test
    void defaultDataContainsRisingFallingAndFlatCases() {
        FakeIntradayBarAdapter adapter = new FakeIntradayBarAdapter();

        assertThat(adapter.findBars(
                "005930",
                TRADE_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                BarInterval.ONE_MINUTE
        )).isNotEmpty();
        assertThat(adapter.findBars(
                "000660",
                TRADE_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                BarInterval.ONE_MINUTE
        )).isNotEmpty();
        assertThat(adapter.findBars(
                "035420",
                TRADE_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                BarInterval.ONE_MINUTE
        )).isNotEmpty();
    }

    private static IntradayBar bar(String time) {
        return new IntradayBar(
                "005930",
                TRADE_DATE,
                LocalTime.parse(time),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(101),
                BigDecimal.valueOf(99),
                BigDecimal.valueOf(100),
                1_000,
                BigDecimal.valueOf(100_000),
                BigDecimal.valueOf(100)
        );
    }
}
