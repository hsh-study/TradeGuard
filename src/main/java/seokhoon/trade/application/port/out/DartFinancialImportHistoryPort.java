package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.DartFinancialImportHistory;

import java.util.List;

public interface DartFinancialImportHistoryPort {
    DartFinancialImportHistory save(DartFinancialImportHistory value);
    List<DartFinancialImportHistory> findHistoriesByStockCode(String stockCode);
}
