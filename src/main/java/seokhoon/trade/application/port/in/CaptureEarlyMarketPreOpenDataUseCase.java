package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface CaptureEarlyMarketPreOpenDataUseCase {
    EarlyMarketDataCaptureResult capturePreOpen(LocalDate tradeDate);
}
