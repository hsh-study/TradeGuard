package seokhoon.trade.adapter.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.StockSectorMappingPort;
import seokhoon.trade.domain.market.StockSectorMapping;

import java.util.List;

@Component
public class StockSectorMappingPersistenceAdapter implements StockSectorMappingPort {
    private final StockSectorMappingJpaRepository repository;

    public StockSectorMappingPersistenceAdapter(StockSectorMappingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public StockSectorMapping save(StockSectorMapping mapping) {
        StockSectorMappingEntity entity = repository
                .findByStockCodeAndSectorCode(mapping.stockCode(), mapping.sectorCode())
                .orElseGet(() -> StockSectorMappingEntity.from(mapping));
        entity.update(mapping);
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockSectorMapping> findBySectorCode(String sectorCode) {
        return repository.findBySectorCodeOrderByStockCodeAsc(sectorCode)
                .stream().map(StockSectorMappingEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockSectorMapping> findByStockCode(String stockCode) {
        return repository.findByStockCodeOrderBySectorCodeAsc(stockCode)
                .stream().map(StockSectorMappingEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockSectorMapping> findAllMappings() {
        return repository.findAll(Sort.by(Sort.Order.asc("stockCode"), Sort.Order.asc("sectorCode")))
                .stream().map(StockSectorMappingEntity::toDomain).toList();
    }
}
