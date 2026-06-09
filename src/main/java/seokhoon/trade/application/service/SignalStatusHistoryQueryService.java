package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.LoadSignalStatusHistoriesUseCase;
import seokhoon.trade.application.port.in.SignalStatusHistoryView;
import seokhoon.trade.application.port.out.SignalStatusHistoryPort;
import seokhoon.trade.application.port.out.SignalStatusHistoryRecord;
import seokhoon.trade.application.port.out.TradingSignalPort;

import java.util.List;

@Service
public class SignalStatusHistoryQueryService implements LoadSignalStatusHistoriesUseCase {
    private final TradingSignalPort tradingSignalPort;
    private final SignalStatusHistoryPort historyPort;

    public SignalStatusHistoryQueryService(
            TradingSignalPort tradingSignalPort,
            SignalStatusHistoryPort historyPort
    ) {
        this.tradingSignalPort = tradingSignalPort;
        this.historyPort = historyPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignalStatusHistoryView> load(long signalId) {
        tradingSignalPort.findById(signalId)
                .orElseThrow(TradingSignalNotFoundException::new);
        return historyPort.findByTradingSignalId(signalId).stream()
                .map(SignalStatusHistoryQueryService::toView)
                .toList();
    }

    private static SignalStatusHistoryView toView(SignalStatusHistoryRecord record) {
        return new SignalStatusHistoryView(
                record.id(),
                record.tradingSignalId(),
                record.fromStatus(),
                record.toStatus(),
                record.reason(),
                record.actor(),
                record.requestCorrelationId(),
                record.createdAt()
        );
    }
}
