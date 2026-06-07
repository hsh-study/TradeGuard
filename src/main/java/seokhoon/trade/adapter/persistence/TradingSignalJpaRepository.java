package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import seokhoon.trade.domain.strategy.SignalType;

import java.time.LocalDate;
import java.util.Optional;

public interface TradingSignalJpaRepository extends JpaRepository<TradingSignalEntity, Long> {
    Optional<TradingSignalEntity> findByStrategyNameAndStockCodeAndSignalDateAndSignalType(
            String strategyName,
            String stockCode,
            LocalDate signalDate,
            SignalType signalType
    );
}
