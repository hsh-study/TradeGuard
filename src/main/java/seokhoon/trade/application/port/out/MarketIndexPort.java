package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.MarketIndex;

import java.time.LocalDate;
import java.util.List;

public interface MarketIndexPort {
    MarketIndex save(MarketIndex index);
    List<MarketIndex> findByTradeDate(LocalDate tradeDate);
}
