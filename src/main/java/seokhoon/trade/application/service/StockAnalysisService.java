package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.AnalysisResult;
import seokhoon.trade.application.port.in.AnalyzeStockUseCase;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.application.port.out.IndicatorSnapshotPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.indicator.TechnicalIndicatorCalculator;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.strategy.ClosingBetStrategy;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.time.LocalDate;
import java.util.List;

@Service
public class StockAnalysisService implements AnalyzeStockUseCase {
    private static final int MINIMUM_PRICE_COUNT = 60;
    private static final int LOOKBACK_YEARS = 1;

    private final DailyPricePort dailyPricePort;
    private final IndicatorSnapshotPort indicatorSnapshotPort;
    private final TradingSignalPort tradingSignalPort;
    private final TechnicalIndicatorCalculator indicatorCalculator;
    private final ClosingBetStrategy closingBetStrategy;

    public StockAnalysisService(
            DailyPricePort dailyPricePort,
            IndicatorSnapshotPort indicatorSnapshotPort,
            TradingSignalPort tradingSignalPort,
            TechnicalIndicatorCalculator indicatorCalculator,
            ClosingBetStrategy closingBetStrategy
    ) {
        this.dailyPricePort = dailyPricePort;
        this.indicatorSnapshotPort = indicatorSnapshotPort;
        this.tradingSignalPort = tradingSignalPort;
        this.indicatorCalculator = indicatorCalculator;
        this.closingBetStrategy = closingBetStrategy;
    }

    @Override
    @Transactional
    public AnalysisResult analyze(String stockCode, LocalDate asOfDate) {
        validate(stockCode, asOfDate);
        List<DailyPrice> prices = dailyPricePort.findByStockCodeAndTradeDateBetween(
                stockCode,
                asOfDate.minusYears(LOOKBACK_YEARS),
                asOfDate
        );
        if (prices.size() < MINIMUM_PRICE_COUNT) {
            throw new InsufficientDailyPriceDataException(MINIMUM_PRICE_COUNT);
        }

        IndicatorSnapshot snapshot = indicatorCalculator.snapshot(stockCode, prices);
        IndicatorSnapshot savedSnapshot = indicatorSnapshotPort.save(snapshot);
        TradingSignal signal = closingBetStrategy.evaluate(prices, savedSnapshot);
        TradingSignal savedSignal = tradingSignalPort.save(signal);
        return new AnalysisResult(savedSnapshot, savedSignal);
    }

    private static void validate(String stockCode, LocalDate asOfDate) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        if (asOfDate == null) {
            throw new IllegalArgumentException("asOfDate must not be null");
        }
    }
}
