package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.ReplayBacktestRunView;
import seokhoon.trade.application.port.in.RunReplayBacktestUseCase;
import seokhoon.trade.domain.research.ReplayBacktestResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/research/backtests/replay")
public class ReplayBacktestController {
    private final RunReplayBacktestUseCase useCase;

    public ReplayBacktestController(RunReplayBacktestUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/closing-bet")
    @ResponseStatus(HttpStatus.CREATED)
    ReplayBacktestRunView runClosingBet(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "1") int holdingDays) {
        return useCase.runClosingBet(from, to, holdingDays);
    }

    @PostMapping("/early-market")
    @ResponseStatus(HttpStatus.CREATED)
    ReplayBacktestRunView runEarlyMarket(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime entryTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime exitTime) {
        return useCase.runEarlyMarket(from, to, entryTime, exitTime);
    }

    @GetMapping("/runs/{runId}")
    ReplayBacktestRunView getRun(@PathVariable long runId) {
        return useCase.getRun(runId);
    }

    @GetMapping("/runs/{runId}/results")
    List<ReplayBacktestResult> getResults(@PathVariable long runId) {
        return useCase.getResults(runId);
    }
}
