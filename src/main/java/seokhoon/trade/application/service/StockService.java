package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.in.FindStocksUseCase;
import seokhoon.trade.application.port.in.RegisterStockUseCase;
import seokhoon.trade.application.port.in.WarmUpDailyPricesAndIndicatorsUseCase;
import seokhoon.trade.application.port.out.StockPort;
import seokhoon.trade.domain.indicator.*;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;

import java.time.*;
import java.util.List;

@Service
public class StockService implements RegisterStockUseCase, FindStocksUseCase {
    private final StockPort stockPort;
    private final WarmUpDailyPricesAndIndicatorsUseCase warmUpUseCase;
    private final Clock clock;

    StockService(StockPort stockPort) {
        this(stockPort,
                WarmUpDailyPricesAndIndicatorsUseCase.noop(),
                Clock.system(ZoneId.of("Asia/Seoul")));
    }

    @Autowired
    public StockService(
            StockPort stockPort,
            WarmUpDailyPricesAndIndicatorsUseCase warmUpUseCase
    ) {
        this(stockPort, warmUpUseCase,
                Clock.system(ZoneId.of("Asia/Seoul")));
    }

    StockService(
            StockPort stockPort,
            WarmUpDailyPricesAndIndicatorsUseCase warmUpUseCase,
            Clock clock
    ) {
        this.stockPort = stockPort;
        this.warmUpUseCase = warmUpUseCase;
        this.clock = clock;
    }

    @Override
    public IndicatorWarmUpResult register(
            String stockCode,
            String stockName,
            Market market
    ) {
        stockPort.save(new Stock(stockCode, stockName, market, true));
        LocalDate baseDate = LocalDate.now(clock);
        try {
            return warmUpUseCase.warmUpStock(stockCode, baseDate);
        } catch (RuntimeException exception) {
            return new IndicatorWarmUpResult(
                    stockCode,
                    baseDate,
                    null,
                    null,
                    0,
                    0,
                    false,
                    false,
                    false,
                    List.of(
                            "INDICATOR_WARMUP_FAILED",
                            "STOCK_REGISTRATION_SUCCEEDED"
                    ),
                    IndicatorWarmUpStatus.FAILED
            );
        }
    }

    @Override
    public List<Stock> findAll() {
        return stockPort.findAll();
    }
}
