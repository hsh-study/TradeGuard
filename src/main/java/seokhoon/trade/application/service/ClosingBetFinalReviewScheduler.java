package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import seokhoon.trade.application.port.in.ReviewClosingBetCandidatesUseCase;
import seokhoon.trade.application.port.out.MarketCalendarPort;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class ClosingBetFinalReviewScheduler {
    private static final Logger log = LoggerFactory.getLogger(ClosingBetFinalReviewScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LIMIT = 5;

    private final ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase;
    private final MarketCalendarPort marketCalendarPort;
    private final Clock clock;

    @Autowired
    public ClosingBetFinalReviewScheduler(
            ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort
    ) {
        this(reviewClosingBetCandidatesUseCase, marketCalendarPort, Clock.system(SEOUL));
    }

    ClosingBetFinalReviewScheduler(
            ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            Clock clock
    ) {
        this.reviewClosingBetCandidatesUseCase = reviewClosingBetCandidatesUseCase;
        this.marketCalendarPort = marketCalendarPort;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 15 * * MON-FRI", zone = "Asia/Seoul")
    public void reviewAtMarketLateAfternoon() {
        LocalDate tradeDate = LocalDate.now(clock);
        if (!marketCalendarPort.isTradingDay(tradeDate)) {
            log.info("Skipping 15:00 closing bet final review on non-trading day: {}", tradeDate);
            return;
        }
        reviewClosingBetCandidatesUseCase.review(tradeDate, DEFAULT_LIMIT);
    }
}
