package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.adapter.persistence.StockEntity;
import seokhoon.trade.adapter.persistence.StockJpaRepository;
import seokhoon.trade.application.port.in.RegisterStockUseCase;
import seokhoon.trade.domain.stock.Market;

import java.util.List;

@Service
public class StockService implements RegisterStockUseCase {
    private final StockJpaRepository repository;

    public StockService(StockJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void register(String stockCode, String stockName, Market market) {
        repository.save(new StockEntity(stockCode, stockName, market, true));
    }

    public List<StockEntity> findAll() {
        return repository.findAll();
    }
}
