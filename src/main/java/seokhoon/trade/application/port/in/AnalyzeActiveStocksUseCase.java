package seokhoon.trade.application.port.in;

import java.time.LocalDate;
import java.util.List;

public interface AnalyzeActiveStocksUseCase {
    List<StockAnalysisExecution> analyzeActive(LocalDate asOfDate);
}
