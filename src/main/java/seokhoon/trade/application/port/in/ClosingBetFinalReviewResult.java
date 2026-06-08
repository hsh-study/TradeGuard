package seokhoon.trade.application.port.in;

import java.time.LocalDate;
import java.util.List;

public record ClosingBetFinalReviewResult(
        LocalDate tradeDate,
        int reviewedCount,
        int selectedCount,
        boolean briefingSent,
        String summary,
        List<ClosingBetFinalReviewCandidate> selectedCandidates
) {
}
