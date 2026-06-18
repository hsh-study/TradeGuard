package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.SupplyDemandSnapshotPort;
import seokhoon.trade.domain.market.StockSupplyDemandSnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class SupplyDemandSnapshotPersistenceAdapter implements SupplyDemandSnapshotPort {
    private final StockSupplyDemandSnapshotJpaRepository snapshots;
    public SupplyDemandSnapshotPersistenceAdapter(StockSupplyDemandSnapshotJpaRepository snapshots){this.snapshots=snapshots;}
    @Override @Transactional public StockSupplyDemandSnapshot save(StockSupplyDemandSnapshot v){var e=snapshots.findByStockCodeAndTradeDate(v.stockCode(),v.tradeDate()).orElseGet(StockSupplyDemandSnapshotEntity::new);e.update(v);return snapshots.save(e).toDomain();}
    @Override public Optional<StockSupplyDemandSnapshot> findByStockCodeAndDate(String code,LocalDate date){return snapshots.findByStockCodeAndTradeDate(code,date).map(StockSupplyDemandSnapshotEntity::toDomain);}
    @Override public Optional<StockSupplyDemandSnapshot> findLatestByStockCode(String code){return snapshots.findFirstByStockCodeOrderByTradeDateDesc(code).map(StockSupplyDemandSnapshotEntity::toDomain);}
    @Override public List<StockSupplyDemandSnapshot> findByTradeDate(LocalDate date){return snapshots.findByTradeDate(date).stream().map(StockSupplyDemandSnapshotEntity::toDomain).toList();}
}
