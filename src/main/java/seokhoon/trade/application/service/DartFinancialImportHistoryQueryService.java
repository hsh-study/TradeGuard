package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.ResearchUseCases.DartFinancialImportHistoryQueryUseCase;
import seokhoon.trade.application.port.out.DartFinancialImportHistoryPort;
import seokhoon.trade.domain.research.DartFinancialImportHistory;

import java.util.List;

@Service
public class DartFinancialImportHistoryQueryService implements DartFinancialImportHistoryQueryUseCase {
    private final DartFinancialImportHistoryPort port;

    public DartFinancialImportHistoryQueryService(DartFinancialImportHistoryPort port) {
        this.port = port;
    }

    @Override
    public List<DartFinancialImportHistory> findByStockCode(String stockCode) {
        return port.findHistoriesByStockCode(stockCode);
    }
}
