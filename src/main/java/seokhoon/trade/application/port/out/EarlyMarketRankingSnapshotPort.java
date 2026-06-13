package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.EarlyMarketRankingSnapshot;

import java.time.LocalDate;
import java.util.List;

public interface EarlyMarketRankingSnapshotPort {
    List<EarlyMarketRankingSnapshot> saveAll(
            List<EarlyMarketRankingSnapshot> snapshots
    );

    List<EarlyMarketRankingSnapshot> findRankings(LocalDate tradeDate);
}
