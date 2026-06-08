package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.stock.Market;

import java.math.BigDecimal;

public record MarketRankingStock(
        String stockCode,
        String stockName,
        Market market,
        BigDecimal currentPrice,
        BigDecimal changeRate,
        BigDecimal tradingValue,
        long volume
) {
}
