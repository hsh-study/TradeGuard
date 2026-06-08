package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.ReviewClosingBetCandidatesUseCase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class ClosingBetFinalReviewScheduler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LIMIT = 5;

    private final ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase;
    private final Clock clock;

    @Autowired
    public ClosingBetFinalReviewScheduler(ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase) {
        this(reviewClosingBetCandidatesUseCase, Clock.system(SEOUL));
    }

    ClosingBetFinalReviewScheduler(
            ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase,
            Clock clock
    ) {
        this.reviewClosingBetCandidatesUseCase = reviewClosingBetCandidatesUseCase;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 15 * * MON-FRI", zone = "Asia/Seoul")
    public void reviewAtMarketLateAfternoon() {
        // TODO: Skip Korean market holidays.
        reviewClosingBetCandidatesUseCase.review(LocalDate.now(clock), DEFAULT_LIMIT);
    }
}
