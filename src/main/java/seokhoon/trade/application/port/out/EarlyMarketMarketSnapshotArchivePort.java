package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.EarlyMarketMarketSnapshot;

import java.time.LocalDate;
import java.util.List;

public interface EarlyMarketMarketSnapshotArchivePort {
    List<EarlyMarketMarketSnapshot> upsertMarketSnapshots(
            List<EarlyMarketMarketSnapshot> snapshots
    );

    List<EarlyMarketMarketSnapshot> findMarketSnapshots(
            LocalDate tradeDate,
            String stockCode
    );
}
