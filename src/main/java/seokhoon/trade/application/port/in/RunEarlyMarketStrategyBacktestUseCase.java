package seokhoon.trade.application.port.in;

public interface RunEarlyMarketStrategyBacktestUseCase {
    EarlyMarketStrategyBacktestResult run(
            RunEarlyMarketStrategyBacktestCommand command
    );
}
