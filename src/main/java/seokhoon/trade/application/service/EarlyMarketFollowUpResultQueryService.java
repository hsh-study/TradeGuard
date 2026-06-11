package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.LoadEarlyMarketFollowUpResultsUseCase;
import seokhoon.trade.application.port.out.EarlyMarketFollowUpResultPort;
import seokhoon.trade.domain.market.EarlyMarketFollowUpRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class EarlyMarketFollowUpResultQueryService
        implements LoadEarlyMarketFollowUpResultsUseCase {
    private final EarlyMarketFollowUpResultPort resultPort;

    public EarlyMarketFollowUpResultQueryService(
            EarlyMarketFollowUpResultPort resultPort
    ) {
        this.resultPort = resultPort;
    }

    @Override
    public List<EarlyMarketFollowUpRecord> findByTradeDate(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        return resultPort.findByTradeDate(tradeDate);
    }

    @Override
    public EarlyMarketFollowUpRecord findBySignalId(long signalId) {
        if (signalId < 1) {
            throw new IllegalArgumentException("signalId must be at least 1");
        }
        return resultPort.findBySignalId(signalId)
                .orElseThrow(() ->
                        new EarlyMarketFollowUpResultNotFoundException(signalId)
                );
    }
}
