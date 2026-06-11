package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpDecision;
import seokhoon.trade.application.port.in.LoadEarlyMarketFollowUpResultsUseCase;
import seokhoon.trade.domain.market.EarlyMarketFollowUpRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scans/early-market/follow-up-results")
public class EarlyMarketFollowUpResultController {
    private final LoadEarlyMarketFollowUpResultsUseCase loadUseCase;

    public EarlyMarketFollowUpResultController(
            LoadEarlyMarketFollowUpResultsUseCase loadUseCase
    ) {
        this.loadUseCase = loadUseCase;
    }

    @GetMapping
    List<FollowUpResultResponse> findByTradeDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tradeDate
    ) {
        return loadUseCase.findByTradeDate(tradeDate).stream()
                .map(FollowUpResultResponse::from)
                .toList();
    }

    @GetMapping("/{signalId}")
    FollowUpResultResponse findBySignalId(@PathVariable long signalId) {
        return FollowUpResultResponse.from(loadUseCase.findBySignalId(signalId));
    }

    public record FollowUpResultResponse(
            long signalId,
            LocalDate tradeDate,
            String stockCode,
            EarlyMarketFollowUpDecision decision,
            int signalScore,
            BigDecimal lastPrice,
            BigDecimal highSince0905,
            BigDecimal drawdownFromHigh,
            Boolean vwapBroken,
            List<String> reasons,
            Instant capturedAt
    ) {
        static FollowUpResultResponse from(EarlyMarketFollowUpRecord record) {
            return new FollowUpResultResponse(
                    record.signalId(),
                    record.tradeDate(),
                    record.stockCode(),
                    record.decision(),
                    record.signalScore(),
                    record.lastPrice(),
                    record.highSince0905(),
                    record.drawdownFromHigh(),
                    record.vwapBroken(),
                    record.reasons(),
                    record.capturedAt()
            );
        }
    }
}
