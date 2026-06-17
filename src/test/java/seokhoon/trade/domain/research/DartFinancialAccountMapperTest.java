package seokhoon.trade.domain.research;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DartFinancialAccountMapperTest {
    @Test
    void mapsReportCodeToFiscalQuarter() {
        assertThat(DartReportCode.fiscalQuarterOf("11013")).isEqualTo(1);
        assertThat(DartReportCode.fiscalQuarterOf("11012")).isEqualTo(2);
        assertThat(DartReportCode.fiscalQuarterOf("11014")).isEqualTo(3);
        assertThat(DartReportCode.fiscalQuarterOf("11011")).isEqualTo(4);
    }

    @Test
    void mapsExactAndNormalizedAccountNames() {
        List<DartFinancialAccount> accounts = List.of(
                new DartFinancialAccount("수익 ( 매출액 )", new BigDecimal("1000")),
                new DartFinancialAccount("영업이익", new BigDecimal("150")),
                new DartFinancialAccount("당기순이익(손실)", new BigDecimal("100")),
                new DartFinancialAccount("자산총계", new BigDecimal("5000")),
                new DartFinancialAccount("부채총계", new BigDecimal("2000")),
                new DartFinancialAccount("자본총계", new BigDecimal("3000")),
                new DartFinancialAccount("영업활동으로 인한 현금흐름", new BigDecimal("120"))
        );

        assertThat(DartFinancialAccountMapper.revenue(accounts)).contains(new BigDecimal("1000"));
        assertThat(DartFinancialAccountMapper.operatingIncome(accounts)).contains(new BigDecimal("150"));
        assertThat(DartFinancialAccountMapper.netIncome(accounts)).contains(new BigDecimal("100"));
        assertThat(DartFinancialAccountMapper.totalAssets(accounts)).contains(new BigDecimal("5000"));
        assertThat(DartFinancialAccountMapper.totalLiabilities(accounts)).contains(new BigDecimal("2000"));
        assertThat(DartFinancialAccountMapper.totalEquity(accounts)).contains(new BigDecimal("3000"));
        assertThat(DartFinancialAccountMapper.operatingCashFlow(accounts)).contains(new BigDecimal("120"));
    }
}
