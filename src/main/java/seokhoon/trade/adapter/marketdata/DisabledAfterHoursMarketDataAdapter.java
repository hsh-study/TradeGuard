package seokhoon.trade.adapter.marketdata;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.AfterHoursMarketDataPort;
import seokhoon.trade.domain.market.AfterHoursQuote;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnAfterHoursProvider("disabled")
public class DisabledAfterHoursMarketDataAdapter implements AfterHoursMarketDataPort {
    @Override
    public List<AfterHoursQuote> findTopAfterHoursMovers(LocalDate tradeDate, int limit) {
        return List.of();
    }

    @Override
    public Optional<AfterHoursQuote> findByStockCode(
            String stockCode,
            LocalDate tradeDate
    ) {
        return Optional.empty();
    }
}
