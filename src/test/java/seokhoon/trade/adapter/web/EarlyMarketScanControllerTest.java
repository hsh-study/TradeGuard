package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.EarlyMarketCandidate;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpCandidate;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpDecision;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpResult;
import seokhoon.trade.application.port.in.EarlyMarketScanResult;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyMarketScanControllerTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);

    @Test
    void runsManualPreOpenScan() {
        EarlyMarketScanController controller = new EarlyMarketScanController(
                (tradeDate, limit) -> result(tradeDate, limit, "pre-open"),
                (tradeDate, limit) -> result(tradeDate, limit, "opening"),
                EarlyMarketScanControllerTest::followUpResult
        );

        var response = controller.preOpen(TRADE_DATE, 10);

        assertThat(response.tradeDate()).isEqualTo(TRADE_DATE);
        assertThat(response.scannedCount()).isEqualTo(10);
        assertThat(response.selectedCount()).isEqualTo(1);
        assertThat(response.summary()).isEqualTo("pre-open");
    }

    @Test
    void runsManualOpeningCompression() {
        EarlyMarketScanController controller = new EarlyMarketScanController(
                (tradeDate, limit) -> result(tradeDate, limit, "pre-open"),
                (tradeDate, limit) -> result(tradeDate, limit, "opening"),
                EarlyMarketScanControllerTest::followUpResult
        );

        var response = controller.opening(TRADE_DATE, 3);

        assertThat(response.scannedCount()).isEqualTo(3);
        assertThat(response.selectedCandidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.sourceSignalId()).isEqualTo(1L);
            assertThat(candidate.signalId()).isEqualTo(2L);
            assertThat(candidate.strategyName()).isEqualTo("EARLY_MARKET_BREAKOUT");
        });
    }

    @Test
    void runsManualFollowUp() {
        EarlyMarketScanController controller = new EarlyMarketScanController(
                (tradeDate, limit) -> result(tradeDate, limit, "pre-open"),
                (tradeDate, limit) -> result(tradeDate, limit, "opening"),
                EarlyMarketScanControllerTest::followUpResult
        );

        var response = controller.followUp(TRADE_DATE);

        assertThat(response.checkedCount()).isEqualTo(1);
        assertThat(response.keepCount()).isEqualTo(1);
        assertThat(response.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.signalId()).isEqualTo(2L);
            assertThat(candidate.decision()).isEqualTo(EarlyMarketFollowUpDecision.KEEP);
        });
    }

    private static EarlyMarketScanResult result(
            LocalDate tradeDate,
            int limit,
            String summary
    ) {
        return new EarlyMarketScanResult(
                tradeDate,
                limit,
                1,
                false,
                summary,
                List.of(new EarlyMarketCandidate(
                        1L,
                        2L,
                        "EARLY_MARKET_BREAKOUT",
                        "005930",
                        90,
                        List.of("ABOVE_VWAP"),
                        List.of(),
                        TradingSignalStatus.CREATED
                ))
        );
    }

    private static EarlyMarketFollowUpResult followUpResult(LocalDate tradeDate) {
        return new EarlyMarketFollowUpResult(
                tradeDate,
                1,
                1,
                0,
                0,
                false,
                List.of(new EarlyMarketFollowUpCandidate(
                        2L,
                        "005930",
                        90,
                        EarlyMarketFollowUpDecision.KEEP,
                        List.of("VWAP_MAINTAINED"),
                        java.math.BigDecimal.valueOf(101),
                        java.math.BigDecimal.valueOf(102),
                        new java.math.BigDecimal("-0.9804"),
                        false
                ))
        );
    }
}
