package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.EarlyMarketFollowUpRecord;

import java.time.LocalDate;
import java.util.List;

public interface LoadEarlyMarketFollowUpResultsUseCase {
    List<EarlyMarketFollowUpRecord> findByTradeDate(LocalDate tradeDate);

    EarlyMarketFollowUpRecord findBySignalId(long signalId);
}
