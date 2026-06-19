package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.ReplayBacktestResult;
import seokhoon.trade.domain.research.ReplayBacktestRun;

import java.util.List;
import java.util.Optional;

public interface ReplayBacktestPort {
    ReplayBacktestRun saveRun(ReplayBacktestRun run);
    List<ReplayBacktestResult> saveResults(List<ReplayBacktestResult> results);
    Optional<ReplayBacktestRun> findRun(long runId);
    default Optional<ReplayBacktestRun> findLatestRun() { return Optional.empty(); }
    List<ReplayBacktestResult> findResults(long runId);
}
