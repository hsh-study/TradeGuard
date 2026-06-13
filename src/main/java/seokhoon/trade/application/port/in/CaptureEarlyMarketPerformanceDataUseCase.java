package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface CaptureEarlyMarketPerformanceDataUseCase {
    EarlyMarketDataCaptureResult capturePerformance(LocalDate tradeDate);
}
