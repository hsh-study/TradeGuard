package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.AnalysisExecutionStatus;
import seokhoon.trade.application.port.in.AnalysisResult;
import seokhoon.trade.application.port.in.AnalyzeActiveStocksUseCase;
import seokhoon.trade.application.port.in.AnalyzeStockUseCase;
import seokhoon.trade.application.port.in.StockAnalysisExecution;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {
    private final AnalyzeStockUseCase analyzeStockUseCase;
    private final AnalyzeActiveStocksUseCase analyzeActiveStocksUseCase;

    public AnalysisController(
            AnalyzeStockUseCase analyzeStockUseCase,
            AnalyzeActiveStocksUseCase analyzeActiveStocksUseCase
    ) {
        this.analyzeStockUseCase = analyzeStockUseCase;
        this.analyzeActiveStocksUseCase = analyzeActiveStocksUseCase;
    }

    @PostMapping("/{stockCode}")
    AnalysisResponse analyze(
            @PathVariable String stockCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate
    ) {
        return AnalysisResponse.from(analyzeStockUseCase.analyze(stockCode, asOfDate));
    }

    @PostMapping("/active")
    BatchAnalysisResponse analyzeActive(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate
    ) {
        List<AnalysisExecutionResponse> executions = analyzeActiveStocksUseCase.analyzeActive(asOfDate).stream()
                .map(AnalysisExecutionResponse::from)
                .toList();
        long analyzedCount = executions.stream()
                .filter(execution -> execution.status() == AnalysisExecutionStatus.ANALYZED)
                .count();
        return new BatchAnalysisResponse(
                asOfDate,
                executions.size(),
                analyzedCount,
                executions.size() - analyzedCount,
                executions
        );
    }

    public record BatchAnalysisResponse(
            LocalDate asOfDate,
            int requestedCount,
            long analyzedCount,
            long skippedCount,
            List<AnalysisExecutionResponse> executions
    ) {
    }

    public record AnalysisExecutionResponse(
            String stockCode,
            String stockName,
            AnalysisExecutionStatus status,
            String message,
            AnalysisResponse analysis
    ) {
        static AnalysisExecutionResponse from(StockAnalysisExecution execution) {
            return new AnalysisExecutionResponse(
                    execution.stockCode(),
                    execution.stockName(),
                    execution.status(),
                    execution.message(),
                    execution.result() == null ? null : AnalysisResponse.from(execution.result())
            );
        }
    }

    public record AnalysisResponse(
            IndicatorResponse indicator,
            TradingSignalResponse signal
    ) {
        static AnalysisResponse from(AnalysisResult result) {
            return new AnalysisResponse(
                    IndicatorResponse.from(result.indicatorSnapshot()),
                    TradingSignalResponse.from(result.tradingSignal())
            );
        }
    }

    public record IndicatorResponse(
            String stockCode,
            LocalDate tradeDate,
            BigDecimal ma5,
            BigDecimal ma20,
            BigDecimal ma60,
            BigDecimal rsi14,
            BigDecimal macd,
            BigDecimal macdSignal,
            BigDecimal macdHistogram,
            BigDecimal bollingerUpper,
            BigDecimal bollingerMiddle,
            BigDecimal bollingerLower
    ) {
        static IndicatorResponse from(IndicatorSnapshot snapshot) {
            return new IndicatorResponse(
                    snapshot.stockCode(),
                    snapshot.tradeDate(),
                    snapshot.ma5(),
                    snapshot.ma20(),
                    snapshot.ma60(),
                    snapshot.rsi14(),
                    snapshot.macd(),
                    snapshot.macdSignal(),
                    snapshot.macdHistogram(),
                    snapshot.bollingerUpper(),
                    snapshot.bollingerMiddle(),
                    snapshot.bollingerLower()
            );
        }
    }

    public record TradingSignalResponse(
            String strategyName,
            String stockCode,
            LocalDate signalDate,
            SignalType signalType,
            int score,
            List<String> reasons,
            List<String> riskReasons,
            TradingSignalStatus status
    ) {
        static TradingSignalResponse from(TradingSignal signal) {
            return new TradingSignalResponse(
                    signal.strategyName(),
                    signal.stockCode(),
                    signal.signalDate(),
                    signal.signalType(),
                    signal.score(),
                    signal.reasons(),
                    signal.riskReasons(),
                    signal.status()
            );
        }
    }
}
