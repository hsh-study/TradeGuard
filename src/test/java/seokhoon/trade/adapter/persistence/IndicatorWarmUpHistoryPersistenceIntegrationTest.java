package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import seokhoon.trade.application.port.out.IndicatorWarmUpHistoryPort;
import seokhoon.trade.domain.indicator.*;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IndicatorWarmUpHistoryPersistenceIntegrationTest {
    @Autowired
    private IndicatorWarmUpHistoryPort historyPort;

    @Test
    void storesAndLoadsWarmupHistoryNewestFirst() {
        String stockCode = "WARMUP-HISTORY-TEST";
        historyPort.save(result(stockCode,
                        IndicatorWarmUpStatus.PARTIAL, 30),
                "insufficient", Instant.parse("2026-06-15T00:00:00Z"));
        historyPort.save(result(stockCode,
                        IndicatorWarmUpStatus.SUCCEEDED, 120),
                null, Instant.parse("2026-06-15T01:00:00Z"));

        assertThat(historyPort.findByStockCode(stockCode))
                .extracting(IndicatorWarmUpHistory::status)
                .containsExactly(
                        IndicatorWarmUpStatus.SUCCEEDED,
                        IndicatorWarmUpStatus.PARTIAL
                );
    }

    private static IndicatorWarmUpResult result(
            String stockCode,
            IndicatorWarmUpStatus status,
            int totalCount
    ) {
        LocalDate baseDate = LocalDate.of(2026, 6, 15);
        return new IndicatorWarmUpResult(
                stockCode,
                baseDate,
                LocalDate.of(2025, 12, 24),
                LocalDate.of(2026, 6, 12),
                totalCount,
                totalCount,
                totalCount >= 60,
                totalCount >= 20,
                totalCount >= 60,
                status == IndicatorWarmUpStatus.SUCCEEDED
                        ? List.of()
                        : List.of("INDICATOR_DATA_INSUFFICIENT"),
                status
        );
    }
}
