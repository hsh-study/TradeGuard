package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.ReplayBacktestResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface RunReplayBacktestUseCase {
    ReplayBacktestRunView runClosingBet(LocalDate fromDate, LocalDate toDate, int holdingDays);
    ReplayBacktestRunView runEarlyMarket(LocalDate fromDate, LocalDate toDate, LocalTime entryTime, LocalTime exitTime);
    ReplayBacktestRunView getRun(long runId);
    List<ReplayBacktestResult> getResults(long runId);
}
