package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.DartFinancialImportHistory;

import java.time.LocalDate;
import java.util.List;

public interface ImportDartFinancialsUseCase {
    DartFinancialImportHistory importStock(String stockCode, int fiscalYear, String reportCode);
    List<DartFinancialImportHistory> importStockRecent(String stockCode, LocalDate baseDate);
    List<DartFinancialImportHistory> importActiveWatchlist(LocalDate baseDate);
}
