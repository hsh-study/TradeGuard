package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.EarlyMarketFollowUpRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EarlyMarketFollowUpResultPort {
    EarlyMarketFollowUpRecord save(EarlyMarketFollowUpRecord result);

    List<EarlyMarketFollowUpRecord> findByTradeDate(LocalDate tradeDate);

    Optional<EarlyMarketFollowUpRecord> findBySignalId(long signalId);

    static EarlyMarketFollowUpResultPort noop() {
        return new EarlyMarketFollowUpResultPort() {
            @Override
            public EarlyMarketFollowUpRecord save(EarlyMarketFollowUpRecord result) {
                return result;
            }

            @Override
            public List<EarlyMarketFollowUpRecord> findByTradeDate(LocalDate tradeDate) {
                return List.of();
            }

            @Override
            public Optional<EarlyMarketFollowUpRecord> findBySignalId(long signalId) {
                return Optional.empty();
            }
        };
    }
}
