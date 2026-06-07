package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.DailyPrice;

import java.time.LocalDate;
import java.util.List;

public interface MarketDataPort {
    List<DailyPrice> fetchDailyPrices(String stockCode, LocalDate from, LocalDate to);
}
