package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockSectorMappingJpaRepository extends JpaRepository<StockSectorMappingEntity, Long> {
    Optional<StockSectorMappingEntity> findByStockCodeAndSectorCode(String stockCode, String sectorCode);
    List<StockSectorMappingEntity> findBySectorCodeOrderByStockCodeAsc(String sectorCode);
    List<StockSectorMappingEntity> findByStockCodeOrderBySectorCodeAsc(String stockCode);
}
