package seokhoon.trade;

import seokhoon.trade.domain.market.DailyPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class TestPrices {
    private TestPrices() {
    }

    public static List<DailyPrice> risingPrices(int days) {
        List<DailyPrice> prices = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < days; i++) {
            BigDecimal close = BigDecimal.valueOf(100 + i);
            prices.add(new DailyPrice("005930", start.plusDays(i), close.subtract(BigDecimal.ONE), close.add(BigDecimal.valueOf(2)),
                    close.subtract(BigDecimal.valueOf(3)), close, 1_000_000L + i, close.multiply(BigDecimal.valueOf(1_000_000L + i))));
        }
        return prices;
    }
}
