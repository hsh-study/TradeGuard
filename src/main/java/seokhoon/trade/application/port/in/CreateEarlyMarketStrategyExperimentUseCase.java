package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

public interface CreateEarlyMarketStrategyExperimentUseCase {
    EarlyMarketStrategyExperiment create(
            CreateEarlyMarketStrategyExperimentCommand command
    );
}
