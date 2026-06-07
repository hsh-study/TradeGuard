package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.DailyPrice;

import java.time.LocalDate;
import java.util.List;

public interface LoadDailyPricesUseCase {
    List<DailyPrice> load(String stockCode, LocalDate from, LocalDate to);
}
