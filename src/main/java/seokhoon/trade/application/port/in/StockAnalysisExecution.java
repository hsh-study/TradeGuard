package seokhoon.trade.application.port.in;

public record StockAnalysisExecution(
        String stockCode,
        String stockName,
        AnalysisExecutionStatus status,
        AnalysisResult result,
        String message
) {
    public static StockAnalysisExecution analyzed(String stockCode, String stockName, AnalysisResult result) {
        return new StockAnalysisExecution(
                stockCode,
                stockName,
                AnalysisExecutionStatus.ANALYZED,
                result,
                null
        );
    }

    public static StockAnalysisExecution skipped(String stockCode, String stockName, String message) {
        return new StockAnalysisExecution(
                stockCode,
                stockName,
                AnalysisExecutionStatus.SKIPPED,
                null,
                message
        );
    }
}
