package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.SignalStatusHistoryPort;
import seokhoon.trade.application.port.out.SignalStatusHistoryRecord;
import seokhoon.trade.domain.strategy.TradingSignalStatus;
import seokhoon.trade.domain.audit.AuditActor;

import java.time.Instant;
import java.util.List;

@Component
public class SignalStatusHistoryPersistenceAdapter implements SignalStatusHistoryPort {
    private final TradingSignalStatusHistoryJpaRepository repository;

    public SignalStatusHistoryPersistenceAdapter(
            TradingSignalStatusHistoryJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public void save(
            long tradingSignalId,
            TradingSignalStatus fromStatus,
            TradingSignalStatus toStatus,
            String reason,
            AuditActor actor,
            String requestCorrelationId,
            Instant createdAt
    ) {
        repository.save(new TradingSignalStatusHistoryEntity(
                tradingSignalId,
                fromStatus,
                toStatus,
                reason,
                actor,
                requestCorrelationId,
                createdAt
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignalStatusHistoryRecord> findByTradingSignalId(long tradingSignalId) {
        return repository.findByTradingSignalIdOrderByCreatedAtAscIdAsc(tradingSignalId)
                .stream()
                .map(TradingSignalStatusHistoryEntity::toRecord)
                .toList();
    }
}
