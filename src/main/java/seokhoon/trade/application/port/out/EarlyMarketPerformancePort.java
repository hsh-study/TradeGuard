package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.EarlyMarketCandidatePerformance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EarlyMarketPerformancePort {
    EarlyMarketCandidatePerformance save(EarlyMarketCandidatePerformance performance);

    List<EarlyMarketCandidatePerformance> findByTradeDate(LocalDate tradeDate);

    Optional<EarlyMarketCandidatePerformance> findBySignalId(long signalId);
}
