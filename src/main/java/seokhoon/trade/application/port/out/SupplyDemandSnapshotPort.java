package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.StockSupplyDemandSnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SupplyDemandSnapshotPort {
    StockSupplyDemandSnapshot save(StockSupplyDemandSnapshot snapshot);
    Optional<StockSupplyDemandSnapshot> findByStockCodeAndDate(String stockCode, LocalDate tradeDate);
    Optional<StockSupplyDemandSnapshot> findLatestByStockCode(String stockCode);
    List<StockSupplyDemandSnapshot> findByTradeDate(LocalDate tradeDate);
}
