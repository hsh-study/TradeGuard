package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.StockSupplyDemandSnapshot;
import java.time.LocalDate;
import java.util.List;

public interface AnalyzeSupplyDemandUseCase {
    StockSupplyDemandSnapshot analyzeStock(String stockCode, LocalDate tradeDate);
    List<StockSupplyDemandSnapshot> analyzeStocks(List<String> stockCodes, LocalDate tradeDate);
    List<StockSupplyDemandSnapshot> analyzeWatchlist(LocalDate tradeDate);
}
