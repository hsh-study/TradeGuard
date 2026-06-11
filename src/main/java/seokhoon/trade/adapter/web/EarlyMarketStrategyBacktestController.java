package seokhoon.trade.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.EarlyMarketStrategyBacktestResult;
import seokhoon.trade.application.port.in.EarlyMarketStrategyParameterOverrides;
import seokhoon.trade.application.port.in.RunEarlyMarketStrategyBacktestCommand;
import seokhoon.trade.application.port.in.RunEarlyMarketStrategyBacktestUseCase;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports/early-market/backtests")
public class EarlyMarketStrategyBacktestController {
    private final RunEarlyMarketStrategyBacktestUseCase backtestUseCase;

    public EarlyMarketStrategyBacktestController(
            RunEarlyMarketStrategyBacktestUseCase backtestUseCase
    ) {
        this.backtestUseCase = backtestUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EarlyMarketStrategyBacktestResult run(
            @Valid @RequestBody BacktestRequest request
    ) {
        return backtestUseCase.run(new RunEarlyMarketStrategyBacktestCommand(
                request.experimentName(),
                request.from(),
                request.to(),
                request.parameterOverrides()
        ));
    }

    public record BacktestRequest(
            @NotBlank
            @Size(max = 100)
            String experimentName,
            @NotNull
            LocalDate from,
            @NotNull
            LocalDate to,
            EarlyMarketStrategyParameterOverrides parameterOverrides
    ) {
    }
}
