package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.domain.market.DailyPrice;

import java.time.LocalDate;
import java.util.List;

@Component
public class DailyPricePersistenceAdapter implements DailyPricePort {
    private final DailyPriceJpaRepository repository;

    public DailyPricePersistenceAdapter(DailyPriceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<DailyPrice> saveAll(List<DailyPrice> dailyPrices) {
        List<DailyPriceEntity> entities = dailyPrices.stream()
                .map(DailyPriceEntity::from)
                .toList();
        return repository.saveAll(entities).stream()
                .map(DailyPriceEntity::toDomain)
                .toList();
    }

    @Override
    public List<DailyPrice> findByStockCodeAndTradeDateBetween(String stockCode, LocalDate from, LocalDate to) {
        return repository.findByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(stockCode, from, to).stream()
                .map(DailyPriceEntity::toDomain)
                .toList();
    }
}
