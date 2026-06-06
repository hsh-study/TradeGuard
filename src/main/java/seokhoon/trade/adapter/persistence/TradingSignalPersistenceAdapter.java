package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.strategy.TradingSignal;

@Component
public class TradingSignalPersistenceAdapter implements TradingSignalPort {
    private final TradingSignalJpaRepository repository;

    public TradingSignalPersistenceAdapter(TradingSignalJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TradingSignal save(TradingSignal tradingSignal) {
        repository.save(TradingSignalEntity.from(tradingSignal));
        return tradingSignal;
    }
}
