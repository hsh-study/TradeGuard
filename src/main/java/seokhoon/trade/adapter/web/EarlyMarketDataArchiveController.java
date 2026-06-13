package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.LoadEarlyMarketDataArchiveUseCase;
import seokhoon.trade.domain.market.EarlyMarketAfterHoursSnapshot;
import seokhoon.trade.domain.market.EarlyMarketDataCapture;
import seokhoon.trade.domain.market.EarlyMarketIntradayBarSnapshot;
import seokhoon.trade.domain.market.EarlyMarketMarketSnapshot;
import seokhoon.trade.domain.market.EarlyMarketRankingSnapshot;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/early-market")
public class EarlyMarketDataArchiveController {
    private final LoadEarlyMarketDataArchiveUseCase loadUseCase;

    public EarlyMarketDataArchiveController(
            LoadEarlyMarketDataArchiveUseCase loadUseCase
    ) {
        this.loadUseCase = loadUseCase;
    }

    @GetMapping("/data-captures")
    List<EarlyMarketDataCapture> loadCaptures(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tradeDate
    ) {
        return loadUseCase.loadCaptures(tradeDate);
    }

    @GetMapping("/ranking-snapshots")
    List<EarlyMarketRankingSnapshot> loadRankings(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tradeDate
    ) {
        return loadUseCase.loadRankings(tradeDate);
    }

    @GetMapping("/after-hours-snapshots")
    List<EarlyMarketAfterHoursSnapshot> loadAfterHours(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tradeDate
    ) {
        return loadUseCase.loadAfterHours(tradeDate);
    }

    @GetMapping("/intraday-bars")
    List<EarlyMarketIntradayBarSnapshot> loadBars(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tradeDate,
            @RequestParam String stockCode
    ) {
        return loadUseCase.loadBars(tradeDate, stockCode);
    }

    @GetMapping("/market-snapshots")
    List<EarlyMarketMarketSnapshot> loadMarketSnapshots(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tradeDate,
            @RequestParam String stockCode
    ) {
        return loadUseCase.loadMarketSnapshots(tradeDate, stockCode);
    }
}
