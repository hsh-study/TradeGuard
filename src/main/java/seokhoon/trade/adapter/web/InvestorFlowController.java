package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.market.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/research")
public class InvestorFlowController {
    private final ImportInvestorFlowsUseCase imports; private final AnalyzeSupplyDemandUseCase analysis;
    private final VerifyInvestorFlowProviderUseCase verification;
    private final StockInvestorFlowPort stockFlows; private final MarketInvestorFlowPort marketFlows;
    private final SupplyDemandSnapshotPort snapshots;
    public InvestorFlowController(ImportInvestorFlowsUseCase imports,AnalyzeSupplyDemandUseCase analysis,
            VerifyInvestorFlowProviderUseCase verification,StockInvestorFlowPort stockFlows,
            MarketInvestorFlowPort marketFlows,SupplyDemandSnapshotPort snapshots){
        this.imports=imports;this.analysis=analysis;this.verification=verification;this.stockFlows=stockFlows;this.marketFlows=marketFlows;this.snapshots=snapshots;}
    @PostMapping(value="/investor-flows/stocks/import-csv",consumes="text/csv")
    InvestorFlowImportHistory importStockCsv(@RequestBody String csv){return imports.importStockCsv(csv);}
    @PostMapping(value="/investor-flows/markets/import-csv",consumes="text/csv")
    InvestorFlowImportHistory importMarketCsv(@RequestBody String csv){return imports.importMarketCsv(csv);}
    @PostMapping("/investor-flows/stocks/import")
    InvestorFlowImportHistory importStock(@RequestParam String stockCode,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tradeDate){return imports.importStock(stockCode,tradeDate);}
    @PostMapping("/investor-flows/markets/import")
    InvestorFlowImportHistory importMarket(@RequestParam InvestorFlowMarket market,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tradeDate){return imports.importMarket(market,tradeDate);}
    @PostMapping("/investor-flows/watchlist/import")
    List<InvestorFlowImportHistory> importWatchlist(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tradeDate){return imports.importWatchlist(tradeDate);}
    @PostMapping("/investor-flows/verify/stock")
    InvestorFlowVerification verifyStock(@RequestParam String stockCode,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tradeDate){return verification.verifyStock(stockCode,tradeDate);}
    @PostMapping("/investor-flows/verify/market")
    InvestorFlowVerification verifyMarket(@RequestParam InvestorFlowMarket market,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tradeDate){return verification.verifyMarket(market,tradeDate);}
    @GetMapping("/investor-flows/import-histories")
    List<InvestorFlowImportHistory> histories(@RequestParam(required=false) String stockCode){return imports.findHistories(stockCode);}
    @GetMapping("/investor-flows/stocks")
    List<StockInvestorFlow> stock(@RequestParam String stockCode,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tradeDate){return stockFlows.findByStockCodeAndDate(stockCode,tradeDate);}
    @GetMapping("/investor-flows/stocks/recent")
    List<StockInvestorFlow> recentStock(@RequestParam String stockCode,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,@RequestParam(defaultValue="20") int days){return stockFlows.findRecentByStockCode(stockCode,endDate,days);}
    @GetMapping("/investor-flows/markets")
    List<MarketInvestorFlow> market(@RequestParam InvestorFlowMarket market,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tradeDate){return marketFlows.findByMarketAndDate(market,tradeDate);}
    @GetMapping("/investor-flows/markets/recent")
    List<MarketInvestorFlow> recentMarket(@RequestParam InvestorFlowMarket market,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,@RequestParam(defaultValue="20") int days){return marketFlows.findRecentByMarket(market,endDate,days);}
    @PostMapping("/supply-demand/analyze")
    StockSupplyDemandSnapshot analyze(@RequestParam String stockCode,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tradeDate){return analysis.analyzeStock(stockCode,tradeDate);}
    @PostMapping("/supply-demand/analyze-watchlist")
    List<StockSupplyDemandSnapshot> analyzeWatchlist(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tradeDate){return analysis.analyzeWatchlist(tradeDate);}
    @GetMapping(value="/supply-demand",params="stockCode")
    List<StockSupplyDemandSnapshot> supplyByStock(@RequestParam String stockCode){return snapshots.findLatestByStockCode(stockCode).stream().toList();}
    @GetMapping(value="/supply-demand",params="tradeDate")
    List<StockSupplyDemandSnapshot> supplyByDate(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tradeDate){return snapshots.findByTradeDate(tradeDate);}
}
