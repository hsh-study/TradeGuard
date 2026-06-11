package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.EarlyMarketStrategyBacktestPeriodSummary;
import seokhoon.trade.application.port.in.EarlyMarketStrategyBacktestResult;
import seokhoon.trade.application.port.in.EarlyMarketStrategyPeriodReport;
import seokhoon.trade.application.port.in.LoadEarlyMarketStrategyPeriodReportUseCase;
import seokhoon.trade.application.port.in.RunEarlyMarketStrategyBacktestCommand;
import seokhoon.trade.application.port.in.RunEarlyMarketStrategyBacktestUseCase;
import seokhoon.trade.application.port.out.EarlyMarketStrategyExperimentPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.config.EarlyMarketStrategyProperties;
import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class EarlyMarketStrategyBacktestService
        implements RunEarlyMarketStrategyBacktestUseCase {
    private static final Logger log =
            LoggerFactory.getLogger(EarlyMarketStrategyBacktestService.class);
    private static final String STORED_SIGNALS_NOT_RECALCULATED =
            "STORED_SIGNALS_NOT_RECALCULATED";
    private static final String PARAMETER_EFFECT_LIMITED_TO_REPORTING =
            "PARAMETER_EFFECT_LIMITED_TO_REPORTING";
    private static final String MISSING_PERFORMANCE_ROWS =
            "MISSING_PERFORMANCE_ROWS";

    private final LoadEarlyMarketStrategyPeriodReportUseCase periodReportUseCase;
    private final EarlyMarketStrategyExperimentPort experimentPort;
    private final EarlyMarketStrategyProperties globalProperties;
    private final EarlyMarketStrategyParameterSupport parameterSupport;
    private final OperationalMetricsPort metricsPort;
    private final Clock clock;

    @Autowired
    public EarlyMarketStrategyBacktestService(
            LoadEarlyMarketStrategyPeriodReportUseCase periodReportUseCase,
            EarlyMarketStrategyExperimentPort experimentPort,
            EarlyMarketStrategyProperties globalProperties,
            EarlyMarketStrategyParameterSupport parameterSupport,
            OperationalMetricsPort metricsPort
    ) {
        this(
                periodReportUseCase,
                experimentPort,
                globalProperties,
                parameterSupport,
                metricsPort,
                Clock.systemUTC()
        );
    }

    EarlyMarketStrategyBacktestService(
            LoadEarlyMarketStrategyPeriodReportUseCase periodReportUseCase,
            EarlyMarketStrategyExperimentPort experimentPort,
            EarlyMarketStrategyProperties globalProperties,
            EarlyMarketStrategyParameterSupport parameterSupport,
            OperationalMetricsPort metricsPort,
            Clock clock
    ) {
        this.periodReportUseCase = periodReportUseCase;
        this.experimentPort = experimentPort;
        this.globalProperties = globalProperties;
        this.parameterSupport = parameterSupport;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Override
    public EarlyMarketStrategyBacktestResult run(
            RunEarlyMarketStrategyBacktestCommand command
    ) {
        validateCommand(command);
        try {
            EarlyMarketStrategyProperties temporaryProperties =
                    parameterSupport.copyAndApply(
                            globalProperties,
                            command.parameterOverrides()
                    );
            EarlyMarketStrategyPeriodReport report =
                    periodReportUseCase.loadPeriodReport(
                            command.from(),
                            command.to()
                    );
            if (report.candidateCount() == 0) {
                metricsPort.recordEarlyMarketBacktest("no_data");
                log.atInfo()
                        .addKeyValue("from", command.from())
                        .addKeyValue("to", command.to())
                        .addKeyValue("result", "no_data")
                        .log("Early market strategy backtest was not saved");
                throw new EarlyMarketStrategyExperimentNoDataException(
                        command.from(),
                        command.to()
                );
            }
            EarlyMarketStrategyExperiment saved = experimentPort.save(
                    experiment(command, report, temporaryProperties)
            );
            List<String> warnings = warnings(report);
            metricsPort.recordEarlyMarketBacktest("saved");
            log.atInfo()
                    .addKeyValue("experimentId", saved.id())
                    .addKeyValue("from", saved.from())
                    .addKeyValue("to", saved.to())
                    .addKeyValue("candidateCount", saved.candidateCount())
                    .addKeyValue("warningCount", warnings.size())
                    .addKeyValue("result", "saved")
                    .log("Early market strategy backtest saved");
            return new EarlyMarketStrategyBacktestResult(
                    saved,
                    summary(report),
                    warnings
            );
        } catch (EarlyMarketStrategyExperimentNoDataException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            metricsPort.recordEarlyMarketBacktest("failure");
            log.atError()
                    .addKeyValue("from", command.from())
                    .addKeyValue("to", command.to())
                    .addKeyValue("result", "failure")
                    .setCause(exception)
                    .log("Early market strategy backtest failed");
            throw exception;
        }
    }

    private EarlyMarketStrategyExperiment experiment(
            RunEarlyMarketStrategyBacktestCommand command,
            EarlyMarketStrategyPeriodReport report,
            EarlyMarketStrategyProperties temporaryProperties
    ) {
        return new EarlyMarketStrategyExperiment(
                null,
                command.experimentName().trim(),
                command.from(),
                command.to(),
                EarlyMarketStrategyParameterSupport.snapshot(
                        temporaryProperties
                ),
                report.candidateCount(),
                report.performanceCapturedCount(),
                report.averageMaxReturnRate(),
                report.averageMaxDrawdownRate(),
                report.winRate(),
                report.bestCandidate() == null
                        ? null
                        : report.bestCandidate().signalId(),
                report.worstCandidate() == null
                        ? null
                        : report.worstCandidate().signalId(),
                clock.instant()
        );
    }

    private static EarlyMarketStrategyBacktestPeriodSummary summary(
            EarlyMarketStrategyPeriodReport report
    ) {
        return new EarlyMarketStrategyBacktestPeriodSummary(
                report.from(),
                report.to(),
                report.tradingDayCount(),
                report.candidateCount(),
                report.performanceCapturedCount(),
                report.excludedFromPerformanceCount(),
                report.averageMaxReturnRate(),
                report.averageMaxDrawdownRate(),
                report.winRate(),
                report.bestCandidate() == null
                        ? null
                        : report.bestCandidate().signalId(),
                report.worstCandidate() == null
                        ? null
                        : report.worstCandidate().signalId(),
                report.dataCompleteness()
        );
    }

    private static List<String> warnings(
            EarlyMarketStrategyPeriodReport report
    ) {
        List<String> warnings = new ArrayList<>();
        warnings.add(STORED_SIGNALS_NOT_RECALCULATED);
        warnings.add(PARAMETER_EFFECT_LIMITED_TO_REPORTING);
        if (report.excludedFromPerformanceCount() > 0) {
            warnings.add(MISSING_PERFORMANCE_ROWS);
        }
        return List.copyOf(warnings);
    }

    private static void validateCommand(
            RunEarlyMarketStrategyBacktestCommand command
    ) {
        Objects.requireNonNull(command, "command");
        if (command.experimentName() == null
                || command.experimentName().isBlank()) {
            throw new IllegalArgumentException(
                    "experimentName must not be blank"
            );
        }
        if (command.experimentName().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "experimentName must not exceed 100 characters"
            );
        }
        Objects.requireNonNull(command.from(), "from");
        Objects.requireNonNull(command.to(), "to");
    }
}
