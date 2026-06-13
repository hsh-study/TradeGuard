package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.EarlyMarketAfterHoursSnapshot;

import java.time.LocalDate;
import java.util.List;

public interface EarlyMarketAfterHoursSnapshotPort {
    List<EarlyMarketAfterHoursSnapshot> upsertAfterHours(
            List<EarlyMarketAfterHoursSnapshot> snapshots
    );

    List<EarlyMarketAfterHoursSnapshot> findAfterHours(LocalDate tradeDate);
}
