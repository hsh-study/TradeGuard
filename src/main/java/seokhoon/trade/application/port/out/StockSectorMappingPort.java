package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.StockSectorMapping;

import java.util.List;

public interface StockSectorMappingPort {
    StockSectorMapping save(StockSectorMapping mapping);
    List<StockSectorMapping> findBySectorCode(String sectorCode);
    List<StockSectorMapping> findByStockCode(String stockCode);
    List<StockSectorMapping> findAllMappings();
}
