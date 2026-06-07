package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class TradingSignalPersistenceAdapter implements TradingSignalPort {
    private final TradingSignalJpaRepository repository;

    public TradingSignalPersistenceAdapter(TradingSignalJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TradingSignal save(TradingSignal tradingSignal) {
        TradingSignalEntity entity = repository.findByStrategyNameAndStockCodeAndSignalDateAndSignalType(
                        tradingSignal.strategyName(),
                        tradingSignal.stockCode(),
                        tradingSignal.signalDate(),
                        tradingSignal.signalType()
                )
                .orElseGet(() -> TradingSignalEntity.from(tradingSignal));
        entity.update(tradingSignal);
        repository.save(entity);
        return tradingSignal;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TradingSignal> find(
            String strategyName,
            String stockCode,
            LocalDate signalDate,
            SignalType signalType
    ) {
        return repository.findByStrategyNameAndStockCodeAndSignalDateAndSignalType(
                        strategyName,
                        stockCode,
                        signalDate,
                        signalType
                )
                .map(TradingSignalEntity::toDomain);
    }
}
