package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.MarketIndexImportHistory;

import java.util.List;

public interface MarketIndexImportHistoryPort {
    MarketIndexImportHistory save(MarketIndexImportHistory history);
    List<MarketIndexImportHistory> findRecentMarketIndexImports(int limit);
}
