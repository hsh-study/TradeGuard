package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.ClosingBetCandidateScanResult;
import seokhoon.trade.application.port.in.ClosingBetPreScanCandidate;
import seokhoon.trade.application.port.in.ScanClosingBetCandidatesUseCase;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scans")
public class ClosingBetScanController {
    private final ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase;

    public ClosingBetScanController(ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase) {
        this.scanClosingBetCandidatesUseCase = scanClosingBetCandidatesUseCase;
    }

    @PostMapping("/closing-bet")
    ClosingBetScanResponse scan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ClosingBetScanResponse.from(scanClosingBetCandidatesUseCase.scan(tradeDate, limit));
    }

    public record ClosingBetScanResponse(
            LocalDate tradeDate,
            int scannedCount,
            int filteredCount,
            int selectedCount,
            boolean briefingSent,
            String summary,
            List<SelectedCandidateResponse> selectedCandidates
    ) {
        static ClosingBetScanResponse from(ClosingBetCandidateScanResult result) {
            return new ClosingBetScanResponse(
                    result.tradeDate(),
                    result.scannedCount(),
                    result.filteredCount(),
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
            Long signalId,
            String strategyName,
            String stockCode,
            int score,
            List<String> reasons,
            List<String> riskReasons,
            TradingSignalStatus status
    ) {
        static SelectedCandidateResponse from(ClosingBetPreScanCandidate candidate) {
            return new SelectedCandidateResponse(
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
}
