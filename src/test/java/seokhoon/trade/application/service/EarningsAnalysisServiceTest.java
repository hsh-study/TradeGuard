package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.EarningsAnalysisPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.QuarterlyFinancialPort;
import seokhoon.trade.application.port.out.ValuationSnapshotPort;
import seokhoon.trade.domain.research.EarningsAnalysisSnapshot;
import seokhoon.trade.domain.research.EarningsAnalysisStatus;
import seokhoon.trade.domain.research.QuarterlyFinancial;
import seokhoon.trade.domain.research.ValuationSnapshot;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EarningsAnalysisServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 6, 15);

    @Test
    void returnsDataInsufficientWhenRecentQuartersAreMissing() {
        InMemoryEarningsAnalysisPort analyses = new InMemoryEarningsAnalysisPort();
        EarningsAnalysisService service = service(List.of(financial(2026, 1, "1000", "100", "80",
                "5000", "1000", "3000", "100", "50")), Optional.empty(), analyses);

        EarningsAnalysisSnapshot result = service.analyzeStock("005930", BASE_DATE);

        assertThat(result.status()).isEqualTo(EarningsAnalysisStatus.DATA_INSUFFICIENT);
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("EARNINGS_DATA_INSUFFICIENT"));
    }

    @Test
    void calculatesGrowthMarginsDebtAndStrongStatus() {
        InMemoryEarningsAnalysisPort analyses = new InMemoryEarningsAnalysisPort();
        EarningsAnalysisService service = service(strongFinancials(),
                Optional.of(valuation("12", "1.2", "1.8")), analyses);

        EarningsAnalysisSnapshot result = service.analyzeStock("005930", BASE_DATE);

        assertThat(result.revenueYoyGrowth()).isEqualByComparingTo("0.2500");
        assertThat(result.operatingMargin()).isEqualByComparingTo("0.1500");
        assertThat(result.netMargin()).isEqualByComparingTo("0.0800");
        assertThat(result.debtRatio()).isEqualByComparingTo("0.5000");
        assertThat(result.valuationScore()).isEqualTo(35);
        assertThat(result.status()).isEqualTo(EarningsAnalysisStatus.STRONG);
    }

    @Test
    void scoresNegativeFcfAndExpensiveValuationAsWeak() {
        InMemoryEarningsAnalysisPort analyses = new InMemoryEarningsAnalysisPort();
        List<QuarterlyFinancial> financials = List.of(
                financial(2026, 1, "900", "-50", "-30", "5000", "7000", "2000", "10", "-20"),
                financial(2025, 4, "1000", "80", "60", "5000", "2000", "3000", "100", "50"),
                financial(2025, 3, "1000", "80", "60", "5000", "2000", "3000", "100", "50"),
                financial(2025, 2, "1000", "80", "60", "5000", "2000", "3000", "100", "50"),
                financial(2025, 1, "1000", "80", "60", "5000", "2000", "3000", "100", "50")
        );
        EarningsAnalysisService service = service(financials,
                Optional.of(valuation("35", "3.5", "6")), analyses);

        EarningsAnalysisSnapshot result = service.analyzeStock("005930", BASE_DATE);

        assertThat(result.reasons()).contains("FCF_NON_POSITIVE", "OPERATING_LOSS", "NET_LOSS",
                "PER_OVER_30", "PBR_OVER_3", "PSR_OVER_5");
        assertThat(result.status()).isEqualTo(EarningsAnalysisStatus.WEAK);
    }

    @Test
    void producesNeutralStatusForModerateScore() {
        InMemoryEarningsAnalysisPort analyses = new InMemoryEarningsAnalysisPort();
        List<QuarterlyFinancial> financials = List.of(
                financial(2026, 1, "1000", "50", "30", "5000", "1000", "3000", "100", "50"),
                financial(2025, 4, "1000", "40", "20", "5000", "1000", "3000", "100", "50"),
                financial(2025, 3, "1000", "40", "20", "5000", "1000", "3000", "100", "50"),
                financial(2025, 2, "1000", "40", "20", "5000", "1000", "3000", "100", "50"),
                financial(2025, 1, "1000", "40", "20", "5000", "1000", "3000", "100", "50")
        );
        EarningsAnalysisService service = service(financials, Optional.empty(), analyses);

        EarningsAnalysisSnapshot result = service.analyzeStock("005930", BASE_DATE);

        assertThat(result.overallScore()).isEqualTo(45);
        assertThat(result.valuationScore()).isNull();
        assertThat(result.status()).isEqualTo(EarningsAnalysisStatus.NEUTRAL);
    }

    private static EarningsAnalysisService service(
            List<QuarterlyFinancial> financials,
            Optional<ValuationSnapshot> valuation,
            EarningsAnalysisPort analyses
    ) {
        QuarterlyFinancialPort financialPort = new QuarterlyFinancialPort() {
            @Override
            public List<QuarterlyFinancial> saveAll(List<QuarterlyFinancial> values) {
                return values;
            }

            @Override
            public List<QuarterlyFinancial> findRecentQuarters(String stockCode, int limit) {
                return financials.stream().limit(limit).toList();
            }

            @Override
            public Optional<QuarterlyFinancial> findByStockCodeAndQuarter(
                    String stockCode,
                    int fiscalYear,
                    int fiscalQuarter
            ) {
                return financials.stream()
                        .filter(value -> value.fiscalYear() == fiscalYear
                                && value.fiscalQuarter() == fiscalQuarter)
                        .findFirst();
            }
        };
        ValuationSnapshotPort valuationPort = new ValuationSnapshotPort() {
            @Override
            public ValuationSnapshot save(ValuationSnapshot value) {
                return value;
            }

            @Override
            public Optional<ValuationSnapshot> findLatestByStockCode(String stockCode, LocalDate baseDate) {
                return valuation;
            }
        };
        return new EarningsAnalysisService(financialPort, valuationPort, analyses,
                OperationalMetricsPort.noop(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static List<QuarterlyFinancial> strongFinancials() {
        return List.of(
                financial(2026, 1, "1250", "187.5", "100", "5000", "1500", "3000", "120", "80"),
                financial(2025, 4, "1000", "100", "80", "5000", "1000", "3000", "100", "50"),
                financial(2025, 3, "1000", "100", "80", "5000", "1000", "3000", "100", "50"),
                financial(2025, 2, "1000", "100", "80", "5000", "1000", "3000", "100", "50"),
                financial(2025, 1, "1000", "100", "80", "5000", "1000", "3000", "100", "50")
        );
    }

    private static QuarterlyFinancial financial(
            int year,
            int quarter,
            String revenue,
            String operatingIncome,
            String netIncome,
            String totalAssets,
            String totalLiabilities,
            String totalEquity,
            String operatingCashFlow,
            String freeCashFlow
    ) {
        return new QuarterlyFinancial(null, "005930", year, quarter,
                new BigDecimal(revenue), new BigDecimal(operatingIncome),
                new BigDecimal(netIncome), new BigDecimal(totalAssets),
                new BigDecimal(totalLiabilities), new BigDecimal(totalEquity),
                new BigDecimal(operatingCashFlow), new BigDecimal(freeCashFlow),
                NOW, NOW);
    }

    private static ValuationSnapshot valuation(String per, String pbr, String psr) {
        return new ValuationSnapshot(null, "005930", BASE_DATE,
                new BigDecimal("500000000000000"), new BigDecimal(per),
                new BigDecimal(pbr), new BigDecimal(psr),
                null, null, null, NOW, NOW);
    }

    private static class InMemoryEarningsAnalysisPort implements EarningsAnalysisPort {
        private final List<EarningsAnalysisSnapshot> values = new ArrayList<>();

        @Override
        public EarningsAnalysisSnapshot save(EarningsAnalysisSnapshot value) {
            values.add(value);
            return value;
        }

        @Override
        public Optional<EarningsAnalysisSnapshot> findByStockCodeAndBaseDate(String stockCode, LocalDate baseDate) {
            return values.stream().filter(value -> value.stockCode().equals(stockCode)
                    && value.baseDate().equals(baseDate)).findFirst();
        }

        @Override
        public Optional<EarningsAnalysisSnapshot> findLatestByStockCode(String stockCode) {
            return values.stream().filter(value -> value.stockCode().equals(stockCode)).findFirst();
        }

        @Override
        public List<EarningsAnalysisSnapshot> findByBaseDate(LocalDate baseDate) {
            return values.stream().filter(value -> value.baseDate().equals(baseDate)).toList();
        }
    }
}
