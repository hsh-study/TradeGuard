package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.LoadTradingSignalsUseCase;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.in.TradingSignalView;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;

import java.util.List;

@Service
public class TradingSignalQueryService implements LoadTradingSignalsUseCase {
    private final TradingSignalQueryPort tradingSignalQueryPort;

    public TradingSignalQueryService(TradingSignalQueryPort tradingSignalQueryPort) {
        this.tradingSignalQueryPort = tradingSignalQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradingSignalView> load(TradingSignalSearchCriteria criteria) {
        if (criteria != null && criteria.stockCode() != null && criteria.stockCode().isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        if (criteria != null && criteria.strategyName() != null && criteria.strategyName().isBlank()) {
            throw new IllegalArgumentException("strategyName must not be blank");
        }
        if (criteria != null && criteria.minScore() != null && criteria.minScore() < 0) {
            throw new IllegalArgumentException("minScore must not be negative");
        }
        return tradingSignalQueryPort.find(criteria).stream()
                .map(TradingSignalQueryService::toView)
                .toList();
    }

    private static TradingSignalView toView(TradingSignalRecord record) {
        return new TradingSignalView(
                record.id(),
                record.strategyName(),
                record.stockCode(),
                record.signalDate(),
                record.signalType(),
                record.score(),
                record.reasons(),
                record.riskReasons(),
                record.status()
        );
    }
}
