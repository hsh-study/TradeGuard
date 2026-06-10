package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface CaptureEarlyMarketPerformancesUseCase {
    EarlyMarketPerformanceCaptureResult capture(LocalDate tradeDate);
}
