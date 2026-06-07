package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.domain.market.DailyPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DailyPriceServiceTest {
    private final InMemoryDailyPricePort dailyPricePort = new InMemoryDailyPricePort();
    private final DailyPriceService dailyPriceService = new DailyPriceService(dailyPricePort);

    @Test
    void savesAndLoadsDailyPricesInDateOrder() {
        DailyPrice later = price(LocalDate.of(2026, 6, 5), "71000");
        DailyPrice earlier = price(LocalDate.of(2026, 6, 4), "70000");

        dailyPriceService.saveAll(List.of(later, earlier));

        assertThat(dailyPriceService.load(
                "005930",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 7)
        )).extracting(DailyPrice::tradeDate)
                .containsExactly(earlier.tradeDate(), later.tradeDate());
    }

    @Test
    void rejectsInvertedDateRange() {
        assertThatThrownBy(() -> dailyPriceService.load(
                "005930",
                LocalDate.of(2026, 6, 7),
                LocalDate.of(2026, 6, 1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from must not be after to");
    }

    @Test
    void copiesSaveInputBeforePassingItToPort() {
        List<DailyPrice> mutablePrices = new ArrayList<>();
        mutablePrices.add(price(LocalDate.of(2026, 6, 5), "71000"));

        dailyPriceService.saveAll(mutablePrices);

        assertThat(dailyPricePort.receivedListIsImmutable).isTrue();
    }

    private static DailyPrice price(LocalDate tradeDate, String closePrice) {
        BigDecimal close = new BigDecimal(closePrice);
        return new DailyPrice(
                "005930",
                tradeDate,
                close,
                close.add(BigDecimal.valueOf(1_000)),
                close.subtract(BigDecimal.valueOf(1_000)),
                close,
                1_000_000L,
                close.multiply(BigDecimal.valueOf(1_000_000L))
        );
    }

    private static class InMemoryDailyPricePort implements DailyPricePort {
        private final List<DailyPrice> prices = new ArrayList<>();
        private boolean receivedListIsImmutable;

        @Override
        public List<DailyPrice> saveAll(List<DailyPrice> dailyPrices) {
            receivedListIsImmutable = isImmutable(dailyPrices);
            for (DailyPrice dailyPrice : dailyPrices) {
                prices.removeIf(saved ->
                        saved.stockCode().equals(dailyPrice.stockCode())
                                && saved.tradeDate().equals(dailyPrice.tradeDate())
                );
                prices.add(dailyPrice);
            }
            return List.copyOf(dailyPrices);
        }

        @Override
        public List<DailyPrice> findByStockCodeAndTradeDateBetween(String stockCode, LocalDate from, LocalDate to) {
            return prices.stream()
                    .filter(price -> price.stockCode().equals(stockCode))
                    .filter(price -> !price.tradeDate().isBefore(from) && !price.tradeDate().isAfter(to))
                    .sorted(Comparator.comparing(DailyPrice::tradeDate))
                    .toList();
        }

        private static boolean isImmutable(List<DailyPrice> dailyPrices) {
            try {
                dailyPrices.add(price(LocalDate.of(2026, 6, 6), "72000"));
                return false;
            } catch (UnsupportedOperationException expected) {
                return true;
            }
        }
    }
}
