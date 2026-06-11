package seokhoon.trade.application.port.in;

public record EarlyMarketReportDataCompleteness(
        int candidateCount,
        int performanceCapturedCount,
        int excludedFromPerformanceCount,
        int maxReturnSampleCount,
        int maxDrawdownSampleCount
) {
}
