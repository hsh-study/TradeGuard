package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ClosingBetCandidateScanResult;
import seokhoon.trade.application.port.in.ScanClosingBetCandidatesUseCase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClosingBetCandidateScanSchedulerTest {
    @Test
    void delegatesScheduledScanToUseCase() {
        RecordingScanUseCase useCase = new RecordingScanUseCase();
        ClosingBetCandidateScanScheduler scheduler = new ClosingBetCandidateScanScheduler(
                useCase,
                Clock.fixed(Instant.parse("2026-06-05T05:00:00Z"), ZoneId.of("Asia/Seoul"))
        );

        scheduler.scanAtMarketAfternoon();

        assertThat(useCase.tradeDate).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(useCase.limit).isEqualTo(5);
    }

    private static class RecordingScanUseCase implements ScanClosingBetCandidatesUseCase {
        private LocalDate tradeDate;
        private int limit;

        @Override
        public ClosingBetCandidateScanResult scan(LocalDate tradeDate, int limit) {
            this.tradeDate = tradeDate;
            this.limit = limit;
            return new ClosingBetCandidateScanResult(tradeDate, 0, 0, 0, false, "summary", List.of());
        }
    }
}
