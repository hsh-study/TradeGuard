package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.StockPort;
import seokhoon.trade.domain.stock.Stock;

import java.util.List;

@Component
public class StockPersistenceAdapter implements StockPort {
    private final StockJpaRepository repository;

    public StockPersistenceAdapter(StockJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Stock save(Stock stock) {
        return repository.save(StockEntity.from(stock)).toDomain();
    }

    @Override
    public List<Stock> findAll() {
        return repository.findAll().stream()
                .map(StockEntity::toDomain)
                .toList();
    }
}
