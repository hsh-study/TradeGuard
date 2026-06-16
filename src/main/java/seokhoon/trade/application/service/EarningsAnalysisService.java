package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.AnalyzeEarningsUseCase;
import seokhoon.trade.application.port.out.EarningsAnalysisPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.QuarterlyFinancialPort;
import seokhoon.trade.application.port.out.ValuationSnapshotPort;
import seokhoon.trade.domain.research.EarningsAnalysisSnapshot;
import seokhoon.trade.domain.research.EarningsAnalysisStatus;
import seokhoon.trade.domain.research.QuarterlyFinancial;
import seokhoon.trade.domain.research.ValuationSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class EarningsAnalysisService implements AnalyzeEarningsUseCase {
    private static final int SCALE = 4;

    private final QuarterlyFinancialPort financialPort;
    private final ValuationSnapshotPort valuationPort;
    private final EarningsAnalysisPort analysisPort;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public EarningsAnalysisService(
            QuarterlyFinancialPort financialPort,
            ValuationSnapshotPort valuationPort,
            EarningsAnalysisPort analysisPort,
            OperationalMetricsPort metrics
    ) {
        this(financialPort, valuationPort, analysisPort, metrics, Clock.systemUTC());
    }

    EarningsAnalysisService(
            QuarterlyFinancialPort financialPort,
            ValuationSnapshotPort valuationPort,
            EarningsAnalysisPort analysisPort,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.financialPort = financialPort;
        this.valuationPort = valuationPort;
        this.analysisPort = analysisPort;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EarningsAnalysisSnapshot analyzeStock(String stockCode, LocalDate baseDate) {
        Objects.requireNonNull(stockCode, "stockCode");
        Objects.requireNonNull(baseDate, "baseDate");
        try {
            EarningsAnalysisSnapshot result = analyze(stockCode, baseDate);
            metrics.recordResearchEarningsAnalysis(
                    result.status() == EarningsAnalysisStatus.DATA_INSUFFICIENT ? "insufficient" : "success");
            return result;
        } catch (RuntimeException exception) {
            metrics.recordResearchEarningsAnalysis("failure");
            throw exception;
        }
    }

    @Override
    @Transactional
    public List<EarningsAnalysisSnapshot> analyzeStocks(List<String> stockCodes, LocalDate baseDate) {
        return stockCodes.stream()
                .map(stockCode -> analyzeStock(stockCode, baseDate))
                .toList();
    }

    private EarningsAnalysisSnapshot analyze(String stockCode, LocalDate baseDate) {
        List<QuarterlyFinancial> quarters = financialPort.findRecentQuarters(stockCode, 5);
        Instant now = clock.instant();
        if (quarters.size() < 4) {
            return saveInsufficient(stockCode, baseDate, quarters, now,
                    "EARNINGS_DATA_INSUFFICIENT 최근 4분기 미만");
        }

        QuarterlyFinancial latest = quarters.get(0);
        Optional<QuarterlyFinancial> priorYearCandidate = quarters.stream()
                .filter(value -> value.fiscalYear() == latest.fiscalYear() - 1
                        && value.fiscalQuarter() == latest.fiscalQuarter())
                .findFirst();
        if (priorYearCandidate.isEmpty()) {
            return saveInsufficient(stockCode, baseDate, quarters, now,
                    "EARNINGS_DATA_INSUFFICIENT 전년동기 비교 데이터 부족");
        }
        QuarterlyFinancial priorYear = priorYearCandidate.orElseThrow();

        BigDecimal revenueYoy = growth(latest.revenue(), priorYear.revenue());
        BigDecimal operatingIncomeYoy = growth(latest.operatingIncome(), priorYear.operatingIncome());
        BigDecimal netIncomeYoy = growth(latest.netIncome(), priorYear.netIncome());
        BigDecimal operatingMargin = ratio(latest.operatingIncome(), latest.revenue());
        BigDecimal netMargin = ratio(latest.netIncome(), latest.revenue());
        BigDecimal debtRatio = ratio(latest.totalLiabilities(), latest.totalEquity());

        List<String> reasons = new ArrayList<>();
        int qualityScore = qualityScore(latest, revenueYoy, operatingIncomeYoy,
                operatingMargin, netMargin, debtRatio, reasons);

        ValuationScore valuation = valuationPort.findLatestByStockCode(stockCode, baseDate)
                .map(value -> valuationScore(value, reasons))
                .orElseGet(() -> {
                    reasons.add("VALUATION_DATA_INSUFFICIENT valuation snapshot 없음");
                    return new ValuationScore(null, null, null, null);
                });
        Integer valuationScore = valuation.score();
        Integer overallScore = valuationScore == null ? qualityScore : qualityScore + valuationScore;
        EarningsAnalysisStatus status = status(overallScore);

        EarningsAnalysisSnapshot snapshot = new EarningsAnalysisSnapshot(
                null, stockCode, baseDate,
                revenueYoy, operatingIncomeYoy, netIncomeYoy,
                operatingMargin, netMargin, debtRatio,
                latest.operatingCashFlow(), latest.freeCashFlow(),
                valuation.per(), valuation.pbr(), valuation.psr(),
                qualityScore, valuationScore, overallScore, status, reasons, now, now);
        return analysisPort.save(snapshot);
    }

    private EarningsAnalysisSnapshot saveInsufficient(
            String stockCode,
            LocalDate baseDate,
            List<QuarterlyFinancial> quarters,
            Instant now,
            String reason
    ) {
        EarningsAnalysisSnapshot snapshot = new EarningsAnalysisSnapshot(
                null, stockCode, baseDate,
                null, null, null, null, null, null,
                quarters.isEmpty() ? null : quarters.get(0).operatingCashFlow(),
                quarters.isEmpty() ? null : quarters.get(0).freeCashFlow(),
                null, null, null, null, null, null,
                EarningsAnalysisStatus.DATA_INSUFFICIENT,
                List.of(reason),
                now, now);
        return analysisPort.save(snapshot);
    }

    private static int qualityScore(
            QuarterlyFinancial latest,
            BigDecimal revenueYoy,
            BigDecimal operatingIncomeYoy,
            BigDecimal operatingMargin,
            BigDecimal netMargin,
            BigDecimal debtRatio,
            List<String> reasons
    ) {
        int score = 0;
        if (greaterThan(revenueYoy, "0.10")) {
            score += 20;
            reasons.add("REVENUE_YOY_OVER_10PCT");
        }
        if (greaterThan(operatingIncomeYoy, "0.10")) {
            score += 20;
            reasons.add("OPERATING_INCOME_YOY_OVER_10PCT");
        }
        if (greaterThan(operatingMargin, "0.10")) {
            score += 15;
            reasons.add("OPERATING_MARGIN_OVER_10PCT");
        }
        if (greaterThan(netMargin, "0.05")) {
            score += 10;
            reasons.add("NET_MARGIN_OVER_5PCT");
        }
        if (latest.operatingCashFlow().signum() > 0) {
            score += 15;
            reasons.add("OPERATING_CASH_FLOW_POSITIVE");
        }
        if (latest.freeCashFlow().signum() > 0) {
            score += 10;
            reasons.add("FCF_POSITIVE");
        } else {
            reasons.add("FCF_NON_POSITIVE");
        }
        if (greaterThan(debtRatio, "2.00")) {
            score -= 20;
            reasons.add("DEBT_RATIO_OVER_200PCT");
        }
        if (latest.operatingIncome().signum() < 0) {
            score -= 30;
            reasons.add("OPERATING_LOSS");
        }
        if (latest.netIncome().signum() < 0) {
            score -= 20;
            reasons.add("NET_LOSS");
        }
        return score;
    }

    private static ValuationScore valuationScore(ValuationSnapshot value, List<String> reasons) {
        int score = 0;
        if (betweenPositive(value.per(), "15")) {
            score += 15;
            reasons.add("PER_0_TO_15");
        } else if (value.per() == null || value.per().signum() <= 0) {
            reasons.add("PER_UNAVAILABLE_OR_LOSS");
        } else if (greaterThan(value.per(), "30")) {
            score -= 10;
            reasons.add("PER_OVER_30");
        }
        if (betweenPositive(value.pbr(), "1.5")) {
            score += 10;
            reasons.add("PBR_0_TO_1_5");
        } else if (greaterThan(value.pbr(), "3")) {
            score -= 10;
            reasons.add("PBR_OVER_3");
        }
        if (betweenPositive(value.psr(), "2")) {
            score += 10;
            reasons.add("PSR_0_TO_2");
        } else if (greaterThan(value.psr(), "5")) {
            score -= 10;
            reasons.add("PSR_OVER_5");
        }
        return new ValuationScore(score, value.per(), value.pbr(), value.psr());
    }

    private static EarningsAnalysisStatus status(Integer overallScore) {
        if (overallScore >= 50) {
            return EarningsAnalysisStatus.STRONG;
        }
        if (overallScore >= 20) {
            return EarningsAnalysisStatus.NEUTRAL;
        }
        return EarningsAnalysisStatus.WEAK;
    }

    private static BigDecimal growth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).divide(previous.abs(), SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return null;
        }
        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private static boolean greaterThan(BigDecimal value, String threshold) {
        return value != null && value.compareTo(new BigDecimal(threshold)) > 0;
    }

    private static boolean betweenPositive(BigDecimal value, String maxInclusive) {
        return value != null && value.signum() > 0
                && value.compareTo(new BigDecimal(maxInclusive)) <= 0;
    }

    private record ValuationScore(Integer score, BigDecimal per, BigDecimal pbr, BigDecimal psr) {
    }
}
