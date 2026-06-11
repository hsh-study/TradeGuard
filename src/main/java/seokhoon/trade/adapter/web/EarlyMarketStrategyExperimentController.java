package seokhoon.trade.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.CreateEarlyMarketStrategyExperimentCommand;
import seokhoon.trade.application.port.in.CreateEarlyMarketStrategyExperimentUseCase;
import seokhoon.trade.application.port.in.CompareEarlyMarketStrategyExperimentsUseCase;
import seokhoon.trade.application.port.in.EarlyMarketStrategyExperimentComparison;
import seokhoon.trade.application.port.in.LoadEarlyMarketStrategyExperimentsUseCase;
import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports/early-market/experiments")
public class EarlyMarketStrategyExperimentController {
    private final CreateEarlyMarketStrategyExperimentUseCase createUseCase;
    private final LoadEarlyMarketStrategyExperimentsUseCase loadUseCase;
    private final CompareEarlyMarketStrategyExperimentsUseCase compareUseCase;

    public EarlyMarketStrategyExperimentController(
            CreateEarlyMarketStrategyExperimentUseCase createUseCase,
            LoadEarlyMarketStrategyExperimentsUseCase loadUseCase,
            CompareEarlyMarketStrategyExperimentsUseCase compareUseCase
    ) {
        this.createUseCase = createUseCase;
        this.loadUseCase = loadUseCase;
        this.compareUseCase = compareUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EarlyMarketStrategyExperiment create(
            @Valid @RequestBody CreateExperimentRequest request
    ) {
        return createUseCase.create(new CreateEarlyMarketStrategyExperimentCommand(
                request.experimentName(),
                request.from(),
                request.to()
        ));
    }

    @GetMapping
    List<EarlyMarketStrategyExperiment> recent(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return loadUseCase.findRecent(limit);
    }

    @GetMapping("/compare")
    EarlyMarketStrategyExperimentComparison compare(
            @RequestParam(name = "ids") List<Long> experimentIds
    ) {
        return compareUseCase.compare(experimentIds);
    }

    @GetMapping("/{id}")
    EarlyMarketStrategyExperiment findById(@PathVariable long id) {
        return loadUseCase.findById(id);
    }

    public record CreateExperimentRequest(
            @NotBlank
            @Size(max = 100)
            String experimentName,
            @NotNull
            LocalDate from,
            @NotNull
            LocalDate to
    ) {
    }
}
