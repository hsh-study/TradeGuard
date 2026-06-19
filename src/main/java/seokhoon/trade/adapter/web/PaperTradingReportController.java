package seokhoon.trade.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.domain.research.PaperTradingReportResult;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/research/paper-trading/reports")
public class PaperTradingReportController {
    private final GeneratePaperTradingReportUseCase useCase;
    public PaperTradingReportController(GeneratePaperTradingReportUseCase useCase) { this.useCase=useCase; }
    @PostMapping("/daily") @ResponseStatus(HttpStatus.CREATED)
    PaperTradingReportView generate(@RequestParam LocalDate tradeDate) { return useCase.generateDailyReport(tradeDate); }
    @GetMapping("/latest") PaperTradingReportView latest(@RequestParam LocalDate tradeDate) { return useCase.getLatestByTradeDate(tradeDate); }
    @GetMapping("/runs/{runId}") PaperTradingReportView run(@PathVariable long runId) { return useCase.getRun(runId); }
    @GetMapping("/runs/{runId}/results") List<PaperTradingReportResult> results(@PathVariable long runId) { return useCase.getResults(runId); }
}
