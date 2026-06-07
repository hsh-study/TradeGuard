package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.DailyPrice;

import java.time.LocalDate;
import java.util.List;

public interface DailyPricePort {
    List<DailyPrice> saveAll(List<DailyPrice> dailyPrices);

    List<DailyPrice> findByStockCodeAndTradeDateBetween(String stockCode, LocalDate from, LocalDate to);
}
