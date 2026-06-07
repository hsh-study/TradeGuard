package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.AnalysisResult;
import seokhoon.trade.application.port.in.AnalyzeActiveStocksUseCase;
import seokhoon.trade.application.port.in.AnalyzeStockUseCase;
import seokhoon.trade.application.port.in.FindStocksUseCase;
import seokhoon.trade.application.port.in.StockAnalysisExecution;
import seokhoon.trade.domain.stock.Stock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ActiveStockAnalysisService implements AnalyzeActiveStocksUseCase {
    private final FindStocksUseCase findStocksUseCase;
    private final AnalyzeStockUseCase analyzeStockUseCase;

    public ActiveStockAnalysisService(
            FindStocksUseCase findStocksUseCase,
            AnalyzeStockUseCase analyzeStockUseCase
    ) {
        this.findStocksUseCase = findStocksUseCase;
        this.analyzeStockUseCase = analyzeStockUseCase;
    }

    @Override
    public List<StockAnalysisExecution> analyzeActive(LocalDate asOfDate) {
        if (asOfDate == null) {
            throw new IllegalArgumentException("asOfDate must not be null");
        }

        List<StockAnalysisExecution> executions = new ArrayList<>();
        for (Stock stock : findStocksUseCase.findAll()) {
            if (!stock.active()) {
                continue;
            }
            executions.add(analyze(stock, asOfDate));
        }
        return List.copyOf(executions);
    }

    private StockAnalysisExecution analyze(Stock stock, LocalDate asOfDate) {
        try {
            AnalysisResult result = analyzeStockUseCase.analyze(stock.stockCode(), asOfDate);
            return StockAnalysisExecution.analyzed(stock.stockCode(), stock.stockName(), result);
        } catch (InsufficientDailyPriceDataException exception) {
            return StockAnalysisExecution.skipped(
                    stock.stockCode(),
                    stock.stockName(),
                    exception.getMessage()
            );
        }
    }
}
