package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ClosingBetFinalReviewCandidate;
import seokhoon.trade.application.port.in.ClosingBetFinalReviewResult;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClosingBetReviewControllerTest {
    @Test
    void runsManualClosingBetReview() {
        LocalDate tradeDate = LocalDate.of(2026, 6, 5);
        ClosingBetReviewController controller = new ClosingBetReviewController((date, limit) -> {
            assertThat(date).isEqualTo(tradeDate);
            assertThat(limit).isEqualTo(5);
            return new ClosingBetFinalReviewResult(
                    date,
                    7,
                    1,
                    true,
                    "15:00 최종 후보 1개",
                    List.of(new ClosingBetFinalReviewCandidate(
                            1L,
                            2L,
                            "CLOSING_BET",
                            "005930",
                            90,
                            List.of("FINAL_REVIEW_15_00", "PRE_SCAN_CONFIRMED"),
                            List.of(),
                            TradingSignalStatus.CREATED
                    ))
            );
        });

        ClosingBetReviewController.ClosingBetReviewResponse response = controller.review(tradeDate, 5);

        assertThat(response.reviewedCount()).isEqualTo(7);
        assertThat(response.selectedCount()).isEqualTo(1);
        assertThat(response.selectedCandidates())
                .singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.preScanSignalId()).isEqualTo(1L);
                    assertThat(candidate.finalSignalId()).isEqualTo(2L);
                    assertThat(candidate.stockCode()).isEqualTo("005930");
                });
    }
}
