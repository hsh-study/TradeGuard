package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.StockPort;
import seokhoon.trade.application.port.in.WarmUpDailyPricesAndIndicatorsUseCase;
import seokhoon.trade.domain.indicator.*;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class StockServiceTest {
    @Test
    void registersAndFindsStocksThroughPort() {
        InMemoryStockPort stockPort = new InMemoryStockPort();
        AtomicBoolean warmUpCalled = new AtomicBoolean();
        WarmUpDailyPricesAndIndicatorsUseCase warmUp =
                new WarmUpDailyPricesAndIndicatorsUseCase() {
                    @Override
                    public IndicatorWarmUpResult warmUpStock(
                            String stockCode, LocalDate baseDate) {
                        warmUpCalled.set(true);
                        return new IndicatorWarmUpResult(
                                stockCode, baseDate, baseDate.minusDays(1),
                                baseDate.minusDays(1), 60, 60, true,
                                true, true, List.of(),
                                IndicatorWarmUpStatus.SUCCEEDED);
                    }

                    @Override
                    public List<IndicatorWarmUpResult> warmUpStocks(
                            List<String> stockCodes, LocalDate baseDate) {
                        return List.of();
                    }
                };
        StockService stockService = new StockService(stockPort, warmUp);

        IndicatorWarmUpResult result = stockService.register(
                "005930", "삼성전자", Market.KOSPI);

        assertThat(stockService.findAll())
                .singleElement()
                .satisfies(stock -> {
                    assertThat(stock.stockCode()).isEqualTo("005930");
                    assertThat(stock.stockName()).isEqualTo("삼성전자");
                    assertThat(stock.market()).isEqualTo(Market.KOSPI);
                    assertThat(stock.active()).isTrue();
                });
        assertThat(warmUpCalled).isTrue();
        assertThat(result.status())
                .isEqualTo(IndicatorWarmUpStatus.SUCCEEDED);
    }

    private static class InMemoryStockPort implements StockPort {
        private final List<Stock> stocks = new ArrayList<>();

        @Override
        public Stock save(Stock stock) {
            stocks.removeIf(saved -> saved.stockCode().equals(stock.stockCode()));
            stocks.add(stock);
            return stock;
        }

        @Override
        public List<Stock> findAll() {
            return List.copyOf(stocks);
        }
    }
}
