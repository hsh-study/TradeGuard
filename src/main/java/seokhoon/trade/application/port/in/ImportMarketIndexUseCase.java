package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.MarketIndexImportHistory;

import java.time.LocalDate;
import java.util.List;

public interface ImportMarketIndexUseCase {
    MarketIndexImportHistory importCsv(String csv);
    MarketIndexImportHistory importProvider(LocalDate tradeDate);
    List<MarketIndexImportHistory> findMarketIndexImportHistories();
}
