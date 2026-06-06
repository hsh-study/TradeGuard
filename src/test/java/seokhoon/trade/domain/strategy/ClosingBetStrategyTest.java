package seokhoon.trade.domain.strategy;

import org.junit.jupiter.api.Test;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.market.DailyPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClosingBetStrategyTest {
    @Test
    void scoresClosingBetCandidate() {
        List<DailyPrice> prices = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < 19; i++) {
            prices.add(new DailyPrice("005930", start.plusDays(i), bd(100), bd(105), bd(98), bd(100), 1_000L, bd(1_000_000)));
        }
        prices.add(new DailyPrice("005930", start.plusDays(19), bd(103), bd(110), bd(100), bd(109), 3_000L, bd(60_000_000_000L)));
        IndicatorSnapshot indicator = new IndicatorSnapshot("005930", start.plusDays(19), bd(105), bd(101), bd(99), bd(55),
                bd(1), bd("0.5"), bd("0.5"), bd(120), bd(100), bd(80));

        TradingSignal signal = new ClosingBetStrategy().evaluate(prices, indicator);

        assertThat(signal.score()).isEqualTo(75);
        assertThat(signal.reasons()).contains("MA5_ABOVE_MA20", "VOLUME_SPIKE_20D_200PCT", "TRADING_VALUE_OVER_50B_KRW");
    }

    private static BigDecimal bd(long value) {
        return BigDecimal.valueOf(value);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
