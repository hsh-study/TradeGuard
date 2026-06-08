package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface ReviewClosingBetCandidatesUseCase {
    ClosingBetFinalReviewResult review(LocalDate tradeDate, int limit);
}
