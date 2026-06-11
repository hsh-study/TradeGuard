package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.CreateEarlyMarketStrategyExperimentCommand;
import seokhoon.trade.application.port.in.CreateEarlyMarketStrategyExperimentUseCase;
import seokhoon.trade.application.port.in.EarlyMarketStrategyPeriodReport;
import seokhoon.trade.application.port.in.LoadEarlyMarketStrategyExperimentsUseCase;
import seokhoon.trade.application.port.in.LoadEarlyMarketStrategyPeriodReportUseCase;
import seokhoon.trade.application.port.out.EarlyMarketStrategyExperimentPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.config.EarlyMarketStrategyProperties;
import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EarlyMarketStrategyExperimentService
        implements CreateEarlyMarketStrategyExperimentUseCase,
        LoadEarlyMarketStrategyExperimentsUseCase {
    private static final Logger log =
            LoggerFactory.getLogger(EarlyMarketStrategyExperimentService.class);
    private static final int MAX_RECENT_LIMIT = 100;

    private final LoadEarlyMarketStrategyPeriodReportUseCase periodReportUseCase;
    private final EarlyMarketStrategyExperimentPort experimentPort;
    private final EarlyMarketStrategyProperties properties;
    private final OperationalMetricsPort metricsPort;
    private final Clock clock;

    @Autowired
    public EarlyMarketStrategyExperimentService(
            LoadEarlyMarketStrategyPeriodReportUseCase periodReportUseCase,
            EarlyMarketStrategyExperimentPort experimentPort,
            EarlyMarketStrategyProperties properties,
            OperationalMetricsPort metricsPort
    ) {
        this(
                periodReportUseCase,
                experimentPort,
                properties,
                metricsPort,
                Clock.systemUTC()
        );
    }

    EarlyMarketStrategyExperimentService(
            LoadEarlyMarketStrategyPeriodReportUseCase periodReportUseCase,
            EarlyMarketStrategyExperimentPort experimentPort,
            EarlyMarketStrategyProperties properties,
            OperationalMetricsPort metricsPort,
            Clock clock
    ) {
        this.periodReportUseCase = periodReportUseCase;
        this.experimentPort = experimentPort;
        this.properties = properties;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Override
    public EarlyMarketStrategyExperiment create(
            CreateEarlyMarketStrategyExperimentCommand command
    ) {
        validateCommand(command);
        try {
            EarlyMarketStrategyPeriodReport report =
                    periodReportUseCase.loadPeriodReport(
                            command.from(),
                            command.to()
                    );
            if (report.candidateCount() == 0) {
                metricsPort.recordEarlyMarketExperiment("no_data");
                log.atInfo()
                        .addKeyValue("from", command.from())
                        .addKeyValue("to", command.to())
                        .addKeyValue("result", "no_data")
                        .log("Early market strategy experiment was not saved");
                throw new EarlyMarketStrategyExperimentNoDataException(
                        command.from(),
                        command.to()
                );
            }
            EarlyMarketStrategyExperiment saved = experimentPort.save(
                    toExperiment(command, report)
            );
            metricsPort.recordEarlyMarketExperiment("saved");
            log.atInfo()
                    .addKeyValue("experimentId", saved.id())
                    .addKeyValue("from", saved.from())
                    .addKeyValue("to", saved.to())
                    .addKeyValue("candidateCount", saved.candidateCount())
                    .addKeyValue("result", "saved")
                    .log("Early market strategy experiment saved");
            return saved;
        } catch (EarlyMarketStrategyExperimentNoDataException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            metricsPort.recordEarlyMarketExperiment("failure");
            log.atError()
                    .addKeyValue("from", command.from())
                    .addKeyValue("to", command.to())
                    .addKeyValue("result", "failure")
                    .setCause(exception)
                    .log("Early market strategy experiment save failed");
            throw exception;
        }
    }

    @Override
    public EarlyMarketStrategyExperiment findById(long id) {
        if (id < 1) {
            throw new IllegalArgumentException("id must be at least 1");
        }
        return experimentPort.findById(id)
                .orElseThrow(() ->
                        new EarlyMarketStrategyExperimentNotFoundException(id));
    }

    @Override
    public List<EarlyMarketStrategyExperiment> findRecent(int limit) {
        if (limit < 1 || limit > MAX_RECENT_LIMIT) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and 100"
            );
        }
        return experimentPort.findRecent(limit);
    }

    private EarlyMarketStrategyExperiment toExperiment(
            CreateEarlyMarketStrategyExperimentCommand command,
            EarlyMarketStrategyPeriodReport report
    ) {
        return new EarlyMarketStrategyExperiment(
                null,
                command.experimentName().trim(),
                command.from(),
                command.to(),
                parameterSnapshot(),
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

    private Map<String, Object> parameterSnapshot() {
        EarlyMarketStrategyProperties.PreOpen preOpen =
                properties.getPreOpen();
        EarlyMarketStrategyProperties.Opening opening =
                properties.getOpening();
        EarlyMarketStrategyProperties.FollowUp followUp =
                properties.getFollowUp();
        EarlyMarketStrategyProperties.PriceAction priceAction =
                properties.getPriceAction();
        return Map.of(
                "preOpen", Map.of(
                        "afterHoursRiseThreshold",
                        preOpen.getAfterHoursRiseThreshold(),
                        "afterHoursRiseScore",
                        preOpen.getAfterHoursRiseScore(),
                        "afterHoursTradingValueThreshold",
                        preOpen.getAfterHoursTradingValueThreshold(),
                        "afterHoursTradingValueScore",
                        preOpen.getAfterHoursTradingValueScore(),
                        "afterHoursOverheatThreshold",
                        preOpen.getAfterHoursOverheatThreshold(),
                        "afterHoursOverheatPenalty",
                        preOpen.getAfterHoursOverheatPenalty(),
                        "afterHoursFallThreshold",
                        preOpen.getAfterHoursFallThreshold(),
                        "afterHoursFallPenalty",
                        preOpen.getAfterHoursFallPenalty()
                ),
                "opening", Map.of(
                        "vwapAboveScore", opening.getVwapAboveScore(),
                        "nearHighScore", opening.getNearHighScore(),
                        "tradingValueScore", opening.getTradingValueScore(),
                        "vwapBrokenPenalty", opening.getVwapBrokenPenalty(),
                        "highDrawdownPenalty", opening.getHighDrawdownPenalty(),
                        "entryThreshold", opening.getEntryThreshold(),
                        "maxCandidates", opening.getMaxCandidates()
                ),
                "followUp", Map.of(
                        "excludeDrawdownFromHigh",
                        followUp.getExcludeDrawdownFromHigh(),
                        "cautionDrawdownFromHigh",
                        followUp.getCautionDrawdownFromHigh(),
                        "excludeWhenLastBelowVwap",
                        followUp.isExcludeWhenLastBelowVwap(),
                        "excludeWhenLastBelowOpeningPrice",
                        followUp.isExcludeWhenLastBelowOpeningPrice(),
                        "cautionWhenPreviousHighNotBroken",
                        followUp.isCautionWhenPreviousHighNotBroken(),
                        "cautionWhenPreviousHighReLost",
                        followUp.isCautionWhenPreviousHighReLost()
                ),
                "priceAction", Map.of(
                        "previousHighBreakoutScore",
                        priceAction.getPreviousHighBreakoutScore(),
                        "previousHighNotBrokenPenalty",
                        priceAction.getPreviousHighNotBrokenPenalty(),
                        "openingPriceHeldScore",
                        priceAction.getOpeningPriceHeldScore(),
                        "openingPriceLostPenalty",
                        priceAction.getOpeningPriceLostPenalty()
                )
        );
    }

    private static void validateCommand(
            CreateEarlyMarketStrategyExperimentCommand command
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
