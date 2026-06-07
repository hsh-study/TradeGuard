package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.DailyPrice;

import java.util.List;

public interface SaveDailyPricesUseCase {
    List<DailyPrice> saveAll(List<DailyPrice> dailyPrices);
}
