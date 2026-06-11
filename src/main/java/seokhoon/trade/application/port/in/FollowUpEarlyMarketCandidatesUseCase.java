package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface FollowUpEarlyMarketCandidatesUseCase {
    EarlyMarketFollowUpResult followUp(LocalDate tradeDate);
}
