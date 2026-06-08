package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ClosingBetCandidateScanResult;
import seokhoon.trade.application.port.in.ClosingBetPreScanCandidate;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClosingBetScanControllerTest {
    @Test
    void runsManualClosingBetScan() {
        LocalDate tradeDate = LocalDate.of(2026, 6, 5);
        ClosingBetScanController controller = new ClosingBetScanController((date, limit) -> {
            assertThat(date).isEqualTo(tradeDate);
            assertThat(limit).isEqualTo(5);
            return new ClosingBetCandidateScanResult(
                    date,
                    12,
                    7,
                    1,
                    true,
                    "14:00 예비 스캔 후보 1개",
                    List.of(new ClosingBetPreScanCandidate(
                            1L,
                            "CLOSING_BET_PRE_SCAN",
                            "005930",
                            90,
                            List.of("MARKET_SCAN_14_00"),
                            List.of(),
                            TradingSignalStatus.CREATED
                    ))
            );
        });

        ClosingBetScanController.ClosingBetScanResponse response = controller.scan(tradeDate, 5);

        assertThat(response.scannedCount()).isEqualTo(12);
        assertThat(response.filteredCount()).isEqualTo(7);
        assertThat(response.selectedCount()).isEqualTo(1);
        assertThat(response.selectedCandidates())
                .singleElement()
                .satisfies(candidate -> assertThat(candidate.stockCode()).isEqualTo("005930"));
    }
}
