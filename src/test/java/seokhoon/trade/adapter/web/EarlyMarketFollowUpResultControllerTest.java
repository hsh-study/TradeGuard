package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpDecision;
import seokhoon.trade.application.port.in.LoadEarlyMarketFollowUpResultsUseCase;
import seokhoon.trade.domain.market.EarlyMarketFollowUpRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyMarketFollowUpResultControllerTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);
    private static final EarlyMarketFollowUpRecord RECORD =
            new EarlyMarketFollowUpRecord(
                    11L,
                    TRADE_DATE,
                    "005930",
                    EarlyMarketFollowUpDecision.KEEP,
                    95,
                    BigDecimal.valueOf(101),
                    BigDecimal.valueOf(103),
                    new BigDecimal("-1.9417"),
                    false,
                    List.of("PREVIOUS_HIGH_HELD"),
                    Instant.parse("2026-06-10T00:20:00Z")
            );

    @Test
    void loadsFollowUpResultsByTradeDateAndSignalId() {
        LoadEarlyMarketFollowUpResultsUseCase useCase =
                new LoadEarlyMarketFollowUpResultsUseCase() {
                    @Override
                    public List<EarlyMarketFollowUpRecord> findByTradeDate(
                            LocalDate tradeDate
                    ) {
                        return List.of(RECORD);
                    }

                    @Override
                    public EarlyMarketFollowUpRecord findBySignalId(long signalId) {
                        return RECORD;
                    }
                };
        EarlyMarketFollowUpResultController controller =
                new EarlyMarketFollowUpResultController(useCase);

        assertThat(controller.findByTradeDate(TRADE_DATE))
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.signalId()).isEqualTo(11L);
                    assertThat(response.decision())
                            .isEqualTo(EarlyMarketFollowUpDecision.KEEP);
                    assertThat(response.reasons()).contains("PREVIOUS_HIGH_HELD");
                });
        assertThat(controller.findBySignalId(11L).stockCode()).isEqualTo("005930");
    }
}
