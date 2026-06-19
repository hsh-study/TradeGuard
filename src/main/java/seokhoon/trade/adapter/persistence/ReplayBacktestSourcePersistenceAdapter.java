package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.ReplayBacktestSourcePort;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.market.EarlyMarketIntradayBarSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class ReplayBacktestSourcePersistenceAdapter implements ReplayBacktestSourcePort {
    private final StockJpaRepository stockRepository;
    private final DailyPriceJpaRepository dailyPriceRepository;
    private final EarlyMarketIntradayBarSnapshotJpaRepository barRepository;

    public ReplayBacktestSourcePersistenceAdapter(StockJpaRepository stockRepository,
                                                   DailyPriceJpaRepository dailyPriceRepository,
                                                   EarlyMarketIntradayBarSnapshotJpaRepository barRepository) {
        this.stockRepository = stockRepository;
        this.dailyPriceRepository = dailyPriceRepository;
        this.barRepository = barRepository;
    }

    @Override
    public Optional<String> findStockName(String stockCode) {
        return stockRepository.findById(stockCode).map(entity -> entity.toDomain().stockName());
    }

    @Override
    public Optional<DailyPrice> findDailyPrice(String stockCode, LocalDate tradeDate) {
        return dailyPriceRepository.findByStockCodeAndTradeDate(stockCode, tradeDate).map(DailyPriceEntity::toDomain);
    }

    @Override
    public Optional<DailyPrice> findNthDailyPriceAfter(String stockCode, LocalDate tradeDate, int tradingDays) {
        List<DailyPriceEntity> prices = dailyPriceRepository
                .findByStockCodeAndTradeDateGreaterThanOrderByTradeDateAsc(stockCode, tradeDate);
        return prices.size() < tradingDays ? Optional.empty() : Optional.of(prices.get(tradingDays - 1).toDomain());
    }

    @Override
    public List<EarlyMarketIntradayBarSnapshot> findIntradayBars(String stockCode, LocalDate tradeDate) {
        return barRepository.findByTradeDateAndStockCodeOrderByBarTimeAsc(tradeDate, stockCode).stream()
                .map(EarlyMarketIntradayBarSnapshotEntity::toDomain).toList();
    }
}
