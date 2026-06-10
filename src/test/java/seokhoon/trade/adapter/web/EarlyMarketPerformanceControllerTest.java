package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.EarlyMarketPerformanceCaptureResult;
import seokhoon.trade.application.port.in.EarlyMarketPerformanceView;
import seokhoon.trade.application.port.in.LoadEarlyMarketPerformancesUseCase;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyMarketPerformanceControllerTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);
    private static final EarlyMarketPerformanceView PERFORMANCE =
            new EarlyMarketPerformanceView(
                    11L,
                    "005930",
                    TRADE_DATE,
                    SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                    105,
                    null,
                    null,
                    null,
                    new BigDecimal("76000"),
                    null,
                    null,
                    false,
                    Instant.parse("2026-06-10T00:31:00Z")
            );

    @Test
    void capturesCandidatePerformances() {
        EarlyMarketPerformanceController controller = controller();

        var response = controller.capture(TRADE_DATE);

        assertThat(response.tradeDate()).isEqualTo(TRADE_DATE);
        assertThat(response.signalCount()).isEqualTo(1);
        assertThat(response.capturedCount()).isEqualTo(1);
        assertThat(response.performances()).singleElement().satisfies(performance -> {
            assertThat(performance.signalId()).isEqualTo(11L);
            assertThat(performance.signalScore()).isEqualTo(105);
            assertThat(performance.priceAt0930()).isEqualByComparingTo("76000");
        });
    }

    @Test
    void loadsPerformancesByTradeDateAndSignalId() {
        EarlyMarketPerformanceController controller = controller();

        assertThat(controller.findByTradeDate(TRADE_DATE))
                .singleElement()
                .extracting(EarlyMarketPerformanceController.PerformanceResponse::signalType)
                .isEqualTo(SignalType.EARLY_MARKET_ENTRY_CANDIDATE);
        assertThat(controller.findBySignalId(11L).stockCode()).isEqualTo("005930");
    }

    private static EarlyMarketPerformanceController controller() {
        LoadEarlyMarketPerformancesUseCase loadUseCase =
                new LoadEarlyMarketPerformancesUseCase() {
                    @Override
                    public List<EarlyMarketPerformanceView> findByTradeDate(
                            LocalDate tradeDate
                    ) {
                        return List.of(PERFORMANCE);
                    }

                    @Override
                    public EarlyMarketPerformanceView findBySignalId(long signalId) {
                        return PERFORMANCE;
                    }
                };
        return new EarlyMarketPerformanceController(
                tradeDate -> new EarlyMarketPerformanceCaptureResult(
                        tradeDate,
                        1,
                        1,
                        List.of(PERFORMANCE)
                ),
                loadUseCase
        );
    }
}
