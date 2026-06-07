package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.AnalysisResult;
import seokhoon.trade.application.port.in.AnalyzeActiveStocksUseCase;
import seokhoon.trade.application.port.in.AnalyzeStockUseCase;
import seokhoon.trade.application.port.in.StockAnalysisExecution;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisControllerTest {
    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 6, 7);

    @Test
    void returnsIndicatorAndSignalForSingleStockAnalysis() {
        AnalyzeStockUseCase analyzeStock = (stockCode, asOfDate) -> analysisResult(stockCode, asOfDate);
        AnalysisController controller = new AnalysisController(analyzeStock, asOfDate -> List.of());

        AnalysisController.AnalysisResponse response = controller.analyze("005930", AS_OF_DATE);

        assertThat(response.indicator().stockCode()).isEqualTo("005930");
        assertThat(response.indicator().tradeDate()).isEqualTo(AS_OF_DATE);
        assertThat(response.signal().score()).isEqualTo(75);
        assertThat(response.signal().reasons()).containsExactly("TEST");
    }

    @Test
    void summarizesAnalyzedAndSkippedActiveStocks() {
        AnalyzeActiveStocksUseCase analyzeActive = asOfDate -> List.of(
                StockAnalysisExecution.analyzed(
                        "005930",
                        "Samsung Electronics",
                        analysisResult("005930", asOfDate)
                ),
                StockAnalysisExecution.skipped(
                        "000660",
                        "SK Hynix",
                        "At least 60 daily prices are required for analysis"
                )
        );
        AnalysisController controller = new AnalysisController(
                (stockCode, asOfDate) -> analysisResult(stockCode, asOfDate),
                analyzeActive
        );

        AnalysisController.BatchAnalysisResponse response = controller.analyzeActive(AS_OF_DATE);

        assertThat(response.requestedCount()).isEqualTo(2);
        assertThat(response.analyzedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.executions().getFirst().analysis()).isNotNull();
        assertThat(response.executions().getLast().analysis()).isNull();
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
}
