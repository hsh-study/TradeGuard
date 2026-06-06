package seokhoon.trade.domain.indicator;

import org.junit.jupiter.api.Test;
import seokhoon.trade.TestPrices;
import seokhoon.trade.domain.market.DailyPrice;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalIndicatorCalculatorTest {
    private final TechnicalIndicatorCalculator calculator = new TechnicalIndicatorCalculator();

    @Test
    void calculatesMovingAverage() {
        BigDecimal ma5 = calculator.movingAverage(TestPrices.risingPrices(10), 5);

        assertThat(ma5).isEqualByComparingTo("107.0000");
    }

    @Test
    void calculatesRsi() {
        BigDecimal rsi = calculator.rsi(TestPrices.risingPrices(15), 14);

        assertThat(rsi).isEqualByComparingTo("100.0000");
    }

    @Test
    void calculatesMacd() {
        Macd macd = calculator.macd(TestPrices.risingPrices(60));

        assertThat(macd.macd()).isPositive();
        assertThat(macd.histogram().abs()).isLessThan(new BigDecimal("1.0000"));
    }

    @Test
    void calculatesBollingerBand() {
        BollingerBand band = calculator.bollingerBand(TestPrices.risingPrices(20), 20);

        assertThat(band.middle()).isEqualByComparingTo("109.5000");
        assertThat(band.upper()).isGreaterThan(band.middle());
        assertThat(band.lower()).isLessThan(band.middle());
    }

    @Test
    void createsSnapshot() {
        List<DailyPrice> prices = TestPrices.risingPrices(70);

        IndicatorSnapshot snapshot = calculator.snapshot("005930", prices);

        assertThat(snapshot.ma60()).isEqualByComparingTo("139.5000");
        assertThat(snapshot.stockCode()).isEqualTo("005930");
    }
}
