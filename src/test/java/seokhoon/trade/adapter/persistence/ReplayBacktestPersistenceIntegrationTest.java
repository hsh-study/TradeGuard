package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.ReplayBacktestPort;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReplayBacktestPersistenceIntegrationTest {
    @Autowired ReplayBacktestPort port;

    @Test
    void storesRunAndResults() {
        Instant now = Instant.parse("2026-06-18T00:00:00Z");
        ReplayBacktestRun run = port.saveRun(new ReplayBacktestRun(null, ReplayBacktestStrategy.CLOSING_BET,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15), ReplayBacktestStatus.RUNNING,
                "{\"holdingDays\":1}", 0, 0, 0, null, null, null, null, now, null));
        port.saveResults(List.of(new ReplayBacktestResult(null, run.id(), LocalDate.of(2026, 6, 2),
                "005930", "삼성전자", ReplayBacktestStrategy.CLOSING_BET, 1, 80,
                List.of("MA5_ABOVE_MA20", "CLOSE_ABOVE_MA20"), List.of(), new BigDecimal("70000"),
                new BigDecimal("71400"), 1, new BigDecimal("2.000000"), ReplayBacktestResultStatus.WIN, now)));

        assertThat(port.findRun(run.id())).isPresent();
        assertThat(port.findResults(run.id())).singleElement().satisfies(result -> {
            assertThat(result.reasons()).containsExactly("MA5_ABOVE_MA20", "CLOSE_ABOVE_MA20");
            assertThat(result.returnRate()).isEqualByComparingTo("2.000000");
        });
    }
}
