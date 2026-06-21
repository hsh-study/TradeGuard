package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.DailyPrice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPricePort {
    List<DailyPrice> saveAll(List<DailyPrice> dailyPrices);

    List<DailyPrice> findByStockCodeAndTradeDateBetween(String stockCode, LocalDate from, LocalDate to);

    default Optional<DailyPrice> findLatestByStockCode(String stockCode) {
        return Optional.empty();
    }
}
