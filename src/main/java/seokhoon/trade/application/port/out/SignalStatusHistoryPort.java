package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.Instant;
import java.util.List;

public interface SignalStatusHistoryPort {
    void save(
            long tradingSignalId,
            TradingSignalStatus fromStatus,
            TradingSignalStatus toStatus,
            String reason,
            Instant createdAt
    );

    List<SignalStatusHistoryRecord> findByTradingSignalId(long tradingSignalId);

    static SignalStatusHistoryPort noop() {
        return new SignalStatusHistoryPort() {
            @Override
            public void save(
                    long tradingSignalId,
                    TradingSignalStatus fromStatus,
                    TradingSignalStatus toStatus,
                    String reason,
                    Instant createdAt
            ) {
            }

            @Override
            public List<SignalStatusHistoryRecord> findByTradingSignalId(long tradingSignalId) {
                return List.of();
            }
        };
    }
}
