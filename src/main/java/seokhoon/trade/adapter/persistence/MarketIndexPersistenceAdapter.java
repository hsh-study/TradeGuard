package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.MarketIndexPort;
import seokhoon.trade.domain.market.MarketIndex;

import java.time.LocalDate;
import java.util.List;

@Component
public class MarketIndexPersistenceAdapter implements MarketIndexPort {
    private final MarketIndexJpaRepository repository;

    public MarketIndexPersistenceAdapter(MarketIndexJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public MarketIndex save(MarketIndex index) {
        MarketIndexEntity entity = repository.findByIndexCodeAndTradeDate(index.indexCode(), index.tradeDate())
                .orElseGet(() -> MarketIndexEntity.from(index));
        entity.update(index);
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketIndex> findByTradeDate(LocalDate tradeDate) {
        return repository.findByTradeDateOrderByIndexCodeAsc(tradeDate)
                .stream().map(MarketIndexEntity::toDomain).toList();
    }
}
