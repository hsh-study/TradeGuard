package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.CompressEarlyMarketOpeningUseCase;
import seokhoon.trade.application.port.in.EarlyMarketCandidate;
import seokhoon.trade.application.port.in.EarlyMarketScanResult;
import seokhoon.trade.application.port.in.ScanEarlyMarketPreOpenUseCase;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scans/early-market")
public class EarlyMarketScanController {
    private final ScanEarlyMarketPreOpenUseCase preOpenUseCase;
    private final CompressEarlyMarketOpeningUseCase openingUseCase;

    public EarlyMarketScanController(
            ScanEarlyMarketPreOpenUseCase preOpenUseCase,
            CompressEarlyMarketOpeningUseCase openingUseCase
    ) {
        this.preOpenUseCase = preOpenUseCase;
        this.openingUseCase = openingUseCase;
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
}
