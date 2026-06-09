package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface CompressEarlyMarketOpeningUseCase {
    EarlyMarketScanResult compress(LocalDate tradeDate, int limit);
}
