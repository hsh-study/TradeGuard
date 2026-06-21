package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.stock.Market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface GetWatchlistPortfolioUseCase {
    List<WatchlistItem> watchlist();
    List<HoldingItem> holdings();
    List<HoldingItem> holdings(long accountId);

    record WatchlistItem(String stockCode, String stockName, Market market,
            boolean active, LocalDate latestTradeDate, BigDecimal closePrice,
            Long volume, BigDecimal per, BigDecimal pbr, List<String> tags) {}

    record HoldingItem(Long positionId, KisEnvironment environment,
            String environmentLabel, String stockCode, String stockName,
            int quantity, BigDecimal averageBuyPrice, BigDecimal buyAmount,
            LocalDate latestTradeDate, BigDecimal closePrice,
            BigDecimal marketValue, BigDecimal unrealizedProfitLoss,
            BigDecimal unrealizedReturnRate, BigDecimal per, BigDecimal pbr,
            Instant openedAt, List<String> tags,
            String source) {}
}
