package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ClosingBetFinalReviewResult;
import seokhoon.trade.application.port.in.ReviewClosingBetCandidatesUseCase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClosingBetFinalReviewSchedulerTest {
    @Test
    void delegatesScheduledReviewToUseCase() {
        RecordingReviewUseCase useCase = new RecordingReviewUseCase();
        ClosingBetFinalReviewScheduler scheduler = new ClosingBetFinalReviewScheduler(
                useCase,
                date -> true,
                Clock.fixed(Instant.parse("2026-06-05T06:00:00Z"), ZoneId.of("Asia/Seoul"))
        );

        scheduler.reviewAtMarketLateAfternoon();

        assertThat(useCase.tradeDate).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(useCase.limit).isEqualTo(5);
        assertThat(useCase.invocationCount).isEqualTo(1);
    }

    @Test
    void skipsScheduledReviewOnNonTradingDay() {
        RecordingReviewUseCase useCase = new RecordingReviewUseCase();
        ClosingBetFinalReviewScheduler scheduler = new ClosingBetFinalReviewScheduler(
                useCase,
                date -> false,
                Clock.fixed(Instant.parse("2026-06-06T06:00:00Z"), ZoneId.of("Asia/Seoul"))
        );

        scheduler.reviewAtMarketLateAfternoon();

        assertThat(useCase.invocationCount).isZero();
    }

    private static class RecordingReviewUseCase implements ReviewClosingBetCandidatesUseCase {
        private LocalDate tradeDate;
        private int limit;
        private int invocationCount;

        @Override
        public ClosingBetFinalReviewResult review(LocalDate tradeDate, int limit) {
            invocationCount++;
            this.tradeDate = tradeDate;
            this.limit = limit;
            return new ClosingBetFinalReviewResult(tradeDate, 0, 0, false, "summary", List.of());
        }
    }
}
