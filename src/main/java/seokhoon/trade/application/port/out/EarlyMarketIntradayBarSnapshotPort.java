package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.EarlyMarketIntradayBarSnapshot;

import java.time.LocalDate;
import java.util.List;

public interface EarlyMarketIntradayBarSnapshotPort {
    List<EarlyMarketIntradayBarSnapshot> upsertBars(
            List<EarlyMarketIntradayBarSnapshot> snapshots
    );

    List<EarlyMarketIntradayBarSnapshot> findBars(
            LocalDate tradeDate,
            String stockCode
    );
}
