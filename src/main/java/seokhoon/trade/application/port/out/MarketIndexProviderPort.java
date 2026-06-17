package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.MarketIndex;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketIndexProviderPort {
    Optional<MarketIndex> fetchIndex(String indexCode, LocalDate tradeDate);
    List<MarketIndex> fetchMajorIndices(LocalDate tradeDate);
}
