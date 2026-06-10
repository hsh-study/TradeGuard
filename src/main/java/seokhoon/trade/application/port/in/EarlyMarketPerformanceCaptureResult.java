package seokhoon.trade.application.port.in;

import java.time.LocalDate;
import java.util.List;

public record EarlyMarketPerformanceCaptureResult(
        LocalDate tradeDate,
        int signalCount,
        int capturedCount,
        List<EarlyMarketPerformanceView> performances
) {
}
