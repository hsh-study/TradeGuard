package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.ClosingBetFinalReviewCandidate;
import seokhoon.trade.application.port.in.ClosingBetFinalReviewResult;
import seokhoon.trade.application.port.in.ReviewClosingBetCandidatesUseCase;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ClosingBetReviewController {
    private final ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase;

    public ClosingBetReviewController(ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase) {
        this.reviewClosingBetCandidatesUseCase = reviewClosingBetCandidatesUseCase;
    }

    @PostMapping("/closing-bet")
    ClosingBetReviewResponse review(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ClosingBetReviewResponse.from(reviewClosingBetCandidatesUseCase.review(tradeDate, limit));
    }

    public record ClosingBetReviewResponse(
            LocalDate tradeDate,
            int reviewedCount,
            int selectedCount,
            boolean briefingSent,
            String summary,
            List<SelectedCandidateResponse> selectedCandidates
    ) {
        static ClosingBetReviewResponse from(ClosingBetFinalReviewResult result) {
            return new ClosingBetReviewResponse(
                    result.tradeDate(),
                    result.reviewedCount(),
                    result.selectedCount(),
                    result.briefingSent(),
                    result.summary(),
                    result.selectedCandidates().stream()
                            .map(SelectedCandidateResponse::from)
                            .toList()
            );
        }
    }

    public record SelectedCandidateResponse(
            Long preScanSignalId,
            Long finalSignalId,
            String strategyName,
            String stockCode,
            int score,
            List<String> reasons,
            List<String> riskReasons,
            TradingSignalStatus status
    ) {
        static SelectedCandidateResponse from(ClosingBetFinalReviewCandidate candidate) {
            return new SelectedCandidateResponse(
                    candidate.preScanSignalId(),
                    candidate.finalSignalId(),
                    candidate.strategyName(),
                    candidate.stockCode(),
                    candidate.score(),
                    candidate.reasons(),
                    candidate.riskReasons(),
                    candidate.status()
            );
        }
    }
}
