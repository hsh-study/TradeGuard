package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface ScanEarlyMarketPreOpenUseCase {
    EarlyMarketScanResult scan(LocalDate tradeDate, int limit);
}
