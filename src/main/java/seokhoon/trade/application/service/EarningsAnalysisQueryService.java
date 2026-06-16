package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.ResearchUseCases.EarningsAnalysisQueryUseCase;
import seokhoon.trade.application.port.out.EarningsAnalysisPort;
import seokhoon.trade.domain.research.EarningsAnalysisSnapshot;

import java.time.LocalDate;
import java.util.List;

@Service
public class EarningsAnalysisQueryService implements EarningsAnalysisQueryUseCase {
    private final EarningsAnalysisPort analysisPort;

    public EarningsAnalysisQueryService(EarningsAnalysisPort analysisPort) {
        this.analysisPort = analysisPort;
    }

    @Override
    public EarningsAnalysisSnapshot findLatestByStockCode(String stockCode) {
        return analysisPort.findLatestByStockCode(stockCode)
                .orElseThrow(() -> new ResearchNotFoundException("Earnings analysis not found: " + stockCode));
    }

    @Override
    public List<EarningsAnalysisSnapshot> findByBaseDate(LocalDate baseDate) {
        return analysisPort.findByBaseDate(baseDate);
    }
}
