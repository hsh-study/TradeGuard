package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface LoadEarlyMarketStrategyReportUseCase {
    EarlyMarketStrategyDailyReport loadDailyReport(LocalDate tradeDate);
}
