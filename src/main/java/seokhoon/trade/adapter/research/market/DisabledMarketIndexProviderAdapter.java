package seokhoon.trade.adapter.research.market;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.MarketIndexProviderPort;
import seokhoon.trade.domain.market.MarketIndex;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class DisabledMarketIndexProviderAdapter implements MarketIndexProviderPort {
    @Override
    public Optional<MarketIndex> fetchIndex(String indexCode, LocalDate tradeDate) {
        return Optional.empty();
    }

    @Override
    public List<MarketIndex> fetchMajorIndices(LocalDate tradeDate) {
        return List.of();
    }
}
