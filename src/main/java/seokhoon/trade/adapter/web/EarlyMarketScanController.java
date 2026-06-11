package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.CompressEarlyMarketOpeningUseCase;
import seokhoon.trade.application.port.in.EarlyMarketCandidate;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpCandidate;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpDecision;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpResult;
import seokhoon.trade.application.port.in.EarlyMarketScanResult;
import seokhoon.trade.application.port.in.FollowUpEarlyMarketCandidatesUseCase;
import seokhoon.trade.application.port.in.ScanEarlyMarketPreOpenUseCase;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scans/early-market")
public class EarlyMarketScanController {
    private final ScanEarlyMarketPreOpenUseCase preOpenUseCase;
    private final CompressEarlyMarketOpeningUseCase openingUseCase;
    private final FollowUpEarlyMarketCandidatesUseCase followUpUseCase;

    public EarlyMarketScanController(
            ScanEarlyMarketPreOpenUseCase preOpenUseCase,
            CompressEarlyMarketOpeningUseCase openingUseCase,
            FollowUpEarlyMarketCandidatesUseCase followUpUseCase
    ) {
        this.preOpenUseCase = preOpenUseCase;
        this.openingUseCase = openingUseCase;
        this.followUpUseCase = followUpUseCase;
    }

    @PostMapping("/pre-open")
    EarlyMarketScanResponse preOpen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return EarlyMarketScanResponse.from(preOpenUseCase.scan(tradeDate, limit));
    }

    @PostMapping("/opening")
    EarlyMarketScanResponse opening(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate,
            @RequestParam(defaultValue = "3") int limit
    ) {
        return EarlyMarketScanResponse.from(openingUseCase.compress(tradeDate, limit));
    }

    @PostMapping("/follow-up")
    EarlyMarketFollowUpResponse followUp(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return EarlyMarketFollowUpResponse.from(followUpUseCase.followUp(tradeDate));
    }

    public record EarlyMarketScanResponse(
            LocalDate tradeDate,
            int scannedCount,
            int selectedCount,
            boolean briefingSent,
            String summary,
            List<CandidateResponse> selectedCandidates
    ) {
        static EarlyMarketScanResponse from(EarlyMarketScanResult result) {
            return new EarlyMarketScanResponse(
                    result.tradeDate(),
                    result.scannedCount(),
                    result.selectedCount(),
                    result.briefingSent(),
                    result.summary(),
                    result.selectedCandidates().stream()
                            .map(CandidateResponse::from)
                            .toList()
            );
        }
    }

    public record CandidateResponse(
            Long sourceSignalId,
            Long signalId,
            String strategyName,
            String stockCode,
            int score,
            List<String> reasons,
            List<String> riskReasons,
            TradingSignalStatus status
    ) {
        static CandidateResponse from(EarlyMarketCandidate candidate) {
            return new CandidateResponse(
                    candidate.sourceSignalId(),
                    candidate.signalId(),
                    candidate.strategyName(),
                    candidate.stockCode(),
                    candidate.score(),
                    candidate.reasons(),
                    candidate.riskReasons(),
                    candidate.status()
            );
        }
    }

    public record EarlyMarketFollowUpResponse(
            LocalDate tradeDate,
            int checkedCount,
            int keepCount,
            int cautionCount,
            int excludeCount,
            boolean briefingSent,
            List<FollowUpCandidateResponse> candidates
    ) {
        static EarlyMarketFollowUpResponse from(EarlyMarketFollowUpResult result) {
            return new EarlyMarketFollowUpResponse(
                    result.tradeDate(),
                    result.checkedCount(),
                    result.keepCount(),
                    result.cautionCount(),
                    result.excludeCount(),
                    result.briefingSent(),
                    result.candidates().stream()
                            .map(FollowUpCandidateResponse::from)
                            .toList()
            );
        }
    }

    public record FollowUpCandidateResponse(
            Long signalId,
            String stockCode,
            int signalScore,
            EarlyMarketFollowUpDecision decision,
            List<String> reasons,
            java.math.BigDecimal lastPrice,
            java.math.BigDecimal highSince0905,
            java.math.BigDecimal drawdownFromHigh,
            Boolean vwapBroken
    ) {
        static FollowUpCandidateResponse from(EarlyMarketFollowUpCandidate candidate) {
            return new FollowUpCandidateResponse(
                    candidate.signalId(),
                    candidate.stockCode(),
                    candidate.signalScore(),
                    candidate.decision(),
                    candidate.reasons(),
                    candidate.lastPrice(),
                    candidate.highSince0905(),
                    candidate.drawdownFromHigh(),
                    candidate.vwapBroken()
            );
        }
    }
}
