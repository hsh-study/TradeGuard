package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.EarningsAnalysisSnapshot;

import java.time.LocalDate;
import java.util.List;

public interface AnalyzeEarningsUseCase {
    EarningsAnalysisSnapshot analyzeStock(String stockCode, LocalDate baseDate);
    List<EarningsAnalysisSnapshot> analyzeStocks(List<String> stockCodes, LocalDate baseDate);
}
