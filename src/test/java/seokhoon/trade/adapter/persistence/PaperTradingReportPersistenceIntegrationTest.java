package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.PaperTradingReportPort;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PaperTradingReportPersistenceIntegrationTest {
    @Autowired PaperTradingReportPort port;
    @Test void storesAndLoadsRunAndResults() {
        Instant now=Instant.parse("2026-06-15T07:10:00Z"); LocalDate date=LocalDate.of(2026,6,15);
        PaperTradingReportRun run=port.saveRun(new PaperTradingReportRun(null,date,PaperTradingReportStatus.RUNNING,
                0,null,0,0,0,null,now,null));
        port.saveResults(List.of(new PaperTradingReportResult(null,run.id(),date,PaperTradingStrategy.EARLY_MARKET,
                "005930","삼성전자",1,10L,80,List.of("VWAP_ABOVE"),List.of(),new BigDecimal("100"),
                new BigDecimal("102"),new BigDecimal("105"),new BigDecimal("98"),new BigDecimal("5"),
                new BigDecimal("-2"),new BigDecimal("2"),PaperTradingResultStatus.WIN,now)));
        assertThat(port.findLatestRun(date)).isPresent();
        assertThat(port.findResults(run.id())).singleElement().satisfies(result -> {
            assertThat(result.reasons()).containsExactly("VWAP_ABOVE");
            assertThat(result.maxAdverseExcursion()).isEqualByComparingTo("-2.000000");
        });
    }
}
