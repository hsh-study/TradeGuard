package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.LoadDailyPricesUseCase;
import seokhoon.trade.application.port.in.SaveDailyPricesUseCase;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.domain.market.DailyPrice;

import java.time.LocalDate;
import java.util.List;

@Service
public class DailyPriceService implements SaveDailyPricesUseCase, LoadDailyPricesUseCase {
    private final DailyPricePort dailyPricePort;

    public DailyPriceService(DailyPricePort dailyPricePort) {
        this.dailyPricePort = dailyPricePort;
    }

    @Override
    @Transactional
    public List<DailyPrice> saveAll(List<DailyPrice> dailyPrices) {
        if (dailyPrices == null) {
            throw new IllegalArgumentException("dailyPrices must not be null");
        }
        return dailyPricePort.saveAll(List.copyOf(dailyPrices));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyPrice> load(String stockCode, LocalDate from, LocalDate to) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("date range must not be null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        return dailyPricePort.findByStockCodeAndTradeDateBetween(stockCode, from, to);
    }
}
