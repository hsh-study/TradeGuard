package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.*;
import java.time.LocalDate;
import java.util.List;

public interface ImportInvestorFlowsUseCase {
    InvestorFlowImportHistory importStock(String stockCode, LocalDate tradeDate);
    InvestorFlowImportHistory importMarket(InvestorFlowMarket market, LocalDate tradeDate);
    List<InvestorFlowImportHistory> importWatchlist(LocalDate tradeDate);
    List<InvestorFlowImportHistory> importRecentWatchlist(LocalDate baseDate);
    InvestorFlowImportHistory importStockCsv(String csv);
    InvestorFlowImportHistory importMarketCsv(String csv);
    List<InvestorFlowImportHistory> findHistories(String stockCode);
}
