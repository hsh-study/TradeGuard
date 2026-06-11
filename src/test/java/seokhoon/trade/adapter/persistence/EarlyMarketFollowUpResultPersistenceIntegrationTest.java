package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpDecision;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.EarlyMarketFollowUpResultPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.service.EarlyMarketPreOpenScanner;
import seokhoon.trade.domain.market.EarlyMarketFollowUpRecord;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EarlyMarketFollowUpResultPersistenceIntegrationTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);

    @Autowired
    private TradingSignalPort tradingSignalPort;

    @Autowired
    private TradingSignalQueryPort tradingSignalQueryPort;

    @Autowired
    private EarlyMarketFollowUpResultPort resultPort;

    @Autowired
    private EarlyMarketFollowUpResultJpaRepository repository;

    @Test
    void updatesExistingFollowUpResultForSameSignalId() {
        tradingSignalPort.save(new TradingSignal(
                EarlyMarketPreOpenScanner.STRATEGY_NAME,
                "005930",
                TRADE_DATE,
                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                90,
                List.of("PREVIOUS_HIGH_BROKEN")
        ));
        long signalId = tradingSignalQueryPort.find(new TradingSignalSearchCriteria(
                "005930",
                TRADE_DATE,
                EarlyMarketPreOpenScanner.STRATEGY_NAME,
                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                null,
                null
        )).getFirst().id();

        resultPort.save(record(
                signalId,
                EarlyMarketFollowUpDecision.KEEP,
                "PREVIOUS_HIGH_HELD"
        ));
        resultPort.save(record(
                signalId,
                EarlyMarketFollowUpDecision.CAUTION,
                "PREVIOUS_HIGH_REENTRY_FAILED"
        ));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(resultPort.findBySignalId(signalId))
                .hasValueSatisfying(result -> {
                    assertThat(result.decision())
                            .isEqualTo(EarlyMarketFollowUpDecision.CAUTION);
                    assertThat(result.reasons())
                            .containsExactly("PREVIOUS_HIGH_REENTRY_FAILED");
                });
        assertThat(resultPort.findByTradeDate(TRADE_DATE))
                .singleElement()
                .extracting(EarlyMarketFollowUpRecord::signalId)
                .isEqualTo(signalId);
    }

    private static EarlyMarketFollowUpRecord record(
            long signalId,
            EarlyMarketFollowUpDecision decision,
            String reason
    ) {
        return new EarlyMarketFollowUpRecord(
                signalId,
                TRADE_DATE,
                "005930",
                decision,
                90,
                BigDecimal.valueOf(101),
                BigDecimal.valueOf(103),
                new BigDecimal("-1.9417"),
                false,
                List.of(reason),
                Instant.parse("2026-06-10T00:20:00Z")
        );
    }
}
