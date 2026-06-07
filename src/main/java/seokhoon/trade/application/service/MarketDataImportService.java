package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.ImportDailyPricesUseCase;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.application.port.out.MarketDataPort;
import seokhoon.trade.domain.market.DailyPrice;

import java.time.LocalDate;
import java.util.List;

@Service
public class MarketDataImportService implements ImportDailyPricesUseCase {
    private final MarketDataPort marketDataPort;
    private final DailyPricePort dailyPricePort;

    public MarketDataImportService(MarketDataPort marketDataPort, DailyPricePort dailyPricePort) {
        this.marketDataPort = marketDataPort;
        this.dailyPricePort = dailyPricePort;
    }

    @Override
    @Transactional
    public List<DailyPrice> importPrices(String stockCode, LocalDate from, LocalDate to) {
        validate(stockCode, from, to);
        List<DailyPrice> fetched = marketDataPort.fetchDailyPrices(stockCode, from, to);
        if (fetched.isEmpty()) {
            return List.of();
        }
        return dailyPricePort.saveAll(fetched);
    }

    private static void validate(String stockCode, LocalDate from, LocalDate to) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("date range must not be null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }
}
