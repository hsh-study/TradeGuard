package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface AnalyzeStockUseCase {
    AnalysisResult analyze(String stockCode, LocalDate asOfDate);
}
