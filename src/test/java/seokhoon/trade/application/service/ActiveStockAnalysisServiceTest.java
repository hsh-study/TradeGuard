package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.AnalysisExecutionStatus;
import seokhoon.trade.application.port.in.AnalysisResult;
import seokhoon.trade.application.port.in.AnalyzeStockUseCase;
import seokhoon.trade.application.port.in.FindStocksUseCase;
import seokhoon.trade.application.port.in.StockAnalysisExecution;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActiveStockAnalysisServiceTest {
    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 6, 7);

    @Test
    void analyzesOnlyActiveStocksAndSkipsInsufficientPriceData() {
        FindStocksUseCase findStocks = () -> List.of(
                new Stock("005930", "Samsung Electronics", Market.KOSPI, true),
                new Stock("000660", "SK Hynix", Market.KOSPI, true),
                new Stock("035420", "Naver", Market.KOSPI, false)
        );
        RecordingAnalyzeStockUseCase analyzeStock = new RecordingAnalyzeStockUseCase();
        ActiveStockAnalysisService service = new ActiveStockAnalysisService(findStocks, analyzeStock);

        List<StockAnalysisExecution> executions = service.analyzeActive(AS_OF_DATE);

        assertThat(analyzeStock.stockCodes).containsExactly("005930", "000660");
        assertThat(executions).extracting(StockAnalysisExecution::status)
                .containsExactly(AnalysisExecutionStatus.ANALYZED, AnalysisExecutionStatus.SKIPPED);
        assertThat(executions.getFirst().result()).isNotNull();
        assertThat(executions.getLast().message())
                .isEqualTo("At least 60 daily prices are required for analysis");
    }

    @Test
    void rejectsNullAnalysisDateBeforeLoadingStocks() {
        FindStocksUseCase findStocks = () -> {
            throw new AssertionError("stocks must not be loaded");
        };
        ActiveStockAnalysisService service = new ActiveStockAnalysisService(
                findStocks,
                (stockCode, asOfDate) -> analysisResult(stockCode, asOfDate)
        );

        assertThatThrownBy(() -> service.analyzeActive(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("asOfDate must not be null");
    }

    private static AnalysisResult analysisResult(String stockCode, LocalDate tradeDate) {
        BigDecimal value = BigDecimal.ONE;
        IndicatorSnapshot snapshot = new IndicatorSnapshot(
                stockCode,
                tradeDate,
                value,
                value,
                value,
                value,
                value,
                value,
                value,
                value,
                value,
                value
        );
        TradingSignal signal = new TradingSignal(
                "CLOSING_BET",
                stockCode,
                tradeDate,
                SignalType.BUY_CANDIDATE,
                75,
                List.of("TEST")
        );
        return new AnalysisResult(snapshot, signal);
    }

    private static class RecordingAnalyzeStockUseCase implements AnalyzeStockUseCase {
        private final List<String> stockCodes = new ArrayList<>();

        @Override
        public AnalysisResult analyze(String stockCode, LocalDate asOfDate) {
            stockCodes.add(stockCode);
            if ("000660".equals(stockCode)) {
                throw new InsufficientDailyPriceDataException(60);
            }
            return analysisResult(stockCode, asOfDate);
        }
    }
}
