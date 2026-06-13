package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.EarlyMarketAfterHoursSnapshot;
import seokhoon.trade.domain.market.EarlyMarketDataCapture;
import seokhoon.trade.domain.market.EarlyMarketIntradayBarSnapshot;
import seokhoon.trade.domain.market.EarlyMarketMarketSnapshot;
import seokhoon.trade.domain.market.EarlyMarketRankingSnapshot;

import java.time.LocalDate;
import java.util.List;

public interface LoadEarlyMarketDataArchiveUseCase {
    List<EarlyMarketDataCapture> loadCaptures(LocalDate tradeDate);

    List<EarlyMarketRankingSnapshot> loadRankings(LocalDate tradeDate);

    List<EarlyMarketAfterHoursSnapshot> loadAfterHours(LocalDate tradeDate);

    List<EarlyMarketIntradayBarSnapshot> loadBars(
            LocalDate tradeDate,
            String stockCode
    );

    List<EarlyMarketMarketSnapshot> loadMarketSnapshots(
            LocalDate tradeDate,
            String stockCode
    );
}
