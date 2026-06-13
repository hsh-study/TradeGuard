package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import seokhoon.trade.domain.market.EarlyMarketSnapshotType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EarlyMarketMarketSnapshotJpaRepository
        extends JpaRepository<EarlyMarketMarketSnapshotEntity, Long> {
    Optional<EarlyMarketMarketSnapshotEntity>
    findByTradeDateAndStockCodeAndCapturedAtAndSnapshotType(
            LocalDate tradeDate,
            String stockCode,
            Instant capturedAt,
            EarlyMarketSnapshotType snapshotType
    );

    List<EarlyMarketMarketSnapshotEntity>
    findByTradeDateAndStockCodeOrderByCapturedAtAscSnapshotTypeAsc(
            LocalDate tradeDate,
            String stockCode
    );
}
