package seokhoon.trade.application.service;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.EarningsAnalysisPort;
import seokhoon.trade.config.EarningsStrategyProperties;
import seokhoon.trade.domain.research.EarningsAnalysisSnapshot;
import seokhoon.trade.domain.research.EarningsAnalysisStatus;

import java.util.ArrayList;
import java.util.List;

@Component
public class EarningsStrategyAdjustment {
    private final EarningsAnalysisPort analysisPort;
    private final EarningsStrategyProperties properties;

    public EarningsStrategyAdjustment(EarningsAnalysisPort analysisPort, EarningsStrategyProperties properties) {
        this.analysisPort = analysisPort;
        this.properties = properties;
    }

    public Assessment assess(String stockCode) {
        if (!properties.isEnabled()) {
            return new Assessment(0, false, List.of());
        }
        return analysisPort.findLatestByStockCode(stockCode)
                .map(this::assess)
                .orElseGet(() -> new Assessment(0, false, List.of("EARNINGS_DATA_UNAVAILABLE")));
    }

    private Assessment assess(EarningsAnalysisSnapshot snapshot) {
        List<String> reasons = new ArrayList<>();
        reasons.add("EARNINGS_STATUS_" + snapshot.status());
        if (snapshot.status() == EarningsAnalysisStatus.STRONG) {
            reasons.add("EARNINGS_STRONG overallScore=" + snapshot.overallScore());
            return new Assessment(properties.getStrongScore(), false, reasons);
        }
        if (snapshot.status() == EarningsAnalysisStatus.WEAK) {
            reasons.add("EARNINGS_WEAK overallScore=" + snapshot.overallScore());
            return new Assessment(properties.getWeakPenalty(), properties.isExcludeWeak(), reasons);
        }
        if (snapshot.status() == EarningsAnalysisStatus.DATA_INSUFFICIENT) {
            reasons.add("EARNINGS_DATA_INSUFFICIENT");
        }
        return new Assessment(0, false, reasons);
    }

    public record Assessment(int scoreAdjustment, boolean excluded, List<String> reasons) {
    }
}
