package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.StockInvestorFlow;
import java.time.LocalDate;
import java.util.List;

public interface StockInvestorFlowPort {
    List<StockInvestorFlow> saveAll(List<StockInvestorFlow> flows);
    List<StockInvestorFlow> findByStockCodeAndDate(String stockCode, LocalDate tradeDate);
    List<StockInvestorFlow> findRecentByStockCode(String stockCode, LocalDate endDate, int days);
}
