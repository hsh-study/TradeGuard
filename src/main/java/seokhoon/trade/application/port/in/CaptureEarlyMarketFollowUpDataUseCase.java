package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface CaptureEarlyMarketFollowUpDataUseCase {
    EarlyMarketDataCaptureResult captureFollowUp(LocalDate tradeDate);
}
