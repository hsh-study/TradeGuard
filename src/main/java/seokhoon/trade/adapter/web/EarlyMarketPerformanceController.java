package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.CaptureEarlyMarketPerformancesUseCase;
import seokhoon.trade.application.port.in.EarlyMarketPerformanceCaptureResult;
import seokhoon.trade.application.port.in.EarlyMarketPerformanceView;
import seokhoon.trade.application.port.in.LoadEarlyMarketPerformancesUseCase;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scans/early-market/performances")
public class EarlyMarketPerformanceController {
    private final CaptureEarlyMarketPerformancesUseCase captureUseCase;
    private final LoadEarlyMarketPerformancesUseCase loadUseCase;

    public EarlyMarketPerformanceController(
            CaptureEarlyMarketPerformancesUseCase captureUseCase,
            LoadEarlyMarketPerformancesUseCase loadUseCase
    ) {
        this.captureUseCase = captureUseCase;
        this.loadUseCase = loadUseCase;
    }

    @PostMapping
    CaptureResponse capture(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tradeDate
    ) {
        return CaptureResponse.from(captureUseCase.capture(tradeDate));
    }

    @GetMapping
    List<PerformanceResponse> findByTradeDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tradeDate
    ) {
        return loadUseCase.findByTradeDate(tradeDate)
                .stream()
                .map(PerformanceResponse::from)
                .toList();
    }

    @GetMapping("/{signalId}")
    PerformanceResponse findBySignalId(@PathVariable long signalId) {
        return PerformanceResponse.from(loadUseCase.findBySignalId(signalId));
    }

    public record CaptureResponse(
            LocalDate tradeDate,
            int signalCount,
            int capturedCount,
            List<PerformanceResponse> performances
    ) {
        static CaptureResponse from(EarlyMarketPerformanceCaptureResult result) {
            return new CaptureResponse(
                    result.tradeDate(),
                    result.signalCount(),
                    result.capturedCount(),
                    result.performances().stream()
                            .map(PerformanceResponse::from)
                            .toList()
            );
        }
    }

    public record PerformanceResponse(
            long signalId,
            String stockCode,
            LocalDate tradeDate,
            SignalType signalType,
            int signalScore,
            BigDecimal entryReferencePrice,
            BigDecimal highUntil0930,
            BigDecimal lowUntil0930,
            BigDecimal priceAt0930,
            BigDecimal maxReturnRateUntil0930,
            BigDecimal maxDrawdownRateUntil0930,
            Boolean vwapBroken,
            Instant capturedAt
    ) {
        static PerformanceResponse from(EarlyMarketPerformanceView view) {
            return new PerformanceResponse(
                    view.signalId(),
                    view.stockCode(),
                    view.tradeDate(),
                    view.signalType(),
                    view.signalScore(),
                    view.entryReferencePrice(),
                    view.highUntil0930(),
                    view.lowUntil0930(),
                    view.priceAt0930(),
                    view.maxReturnRateUntil0930(),
                    view.maxDrawdownRateUntil0930(),
                    view.vwapBroken(),
                    view.capturedAt()
            );
        }
    }
}
