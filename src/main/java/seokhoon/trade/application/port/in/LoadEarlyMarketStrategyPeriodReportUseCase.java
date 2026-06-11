package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface LoadEarlyMarketStrategyPeriodReportUseCase {
    EarlyMarketStrategyPeriodReport loadPeriodReport(LocalDate from, LocalDate to);
}
