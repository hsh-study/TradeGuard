package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface CaptureEarlyMarketOpeningDataUseCase {
    EarlyMarketDataCaptureResult captureOpening(LocalDate tradeDate);
}
