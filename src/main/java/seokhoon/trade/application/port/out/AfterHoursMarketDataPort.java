package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.AfterHoursQuote;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AfterHoursMarketDataPort {
    List<AfterHoursQuote> findTopAfterHoursMovers(LocalDate tradeDate, int limit);

    Optional<AfterHoursQuote> findByStockCode(String stockCode, LocalDate tradeDate);

    static AfterHoursMarketDataPort empty() {
        return new AfterHoursMarketDataPort() {
            @Override
            public List<AfterHoursQuote> findTopAfterHoursMovers(
                    LocalDate tradeDate,
                    int limit
            ) {
                return List.of();
            }

            @Override
            public Optional<AfterHoursQuote> findByStockCode(
                    String stockCode,
                    LocalDate tradeDate
            ) {
                return Optional.empty();
            }
        };
    }
}
