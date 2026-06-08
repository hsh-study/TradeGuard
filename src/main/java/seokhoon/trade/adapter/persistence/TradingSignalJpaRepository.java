package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import seokhoon.trade.domain.strategy.SignalType;

import java.time.LocalDate;
import java.util.Optional;

public interface TradingSignalJpaRepository extends JpaRepository<TradingSignalEntity, Long>,
        JpaSpecificationExecutor<TradingSignalEntity> {
    Optional<TradingSignalEntity> findByStrategyNameAndStockCodeAndSignalDateAndSignalType(
            String strategyName,
            String stockCode,
            LocalDate signalDate,
            SignalType signalType
    );
}
