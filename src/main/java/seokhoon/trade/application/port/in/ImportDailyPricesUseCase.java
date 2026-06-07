package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.DailyPrice;

import java.time.LocalDate;
import java.util.List;

public interface ImportDailyPricesUseCase {
    List<DailyPrice> importPrices(String stockCode, LocalDate from, LocalDate to);
}
