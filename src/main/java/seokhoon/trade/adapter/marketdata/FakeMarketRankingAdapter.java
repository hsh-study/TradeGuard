package seokhoon.trade.adapter.marketdata;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.MarketRankingPort;
import seokhoon.trade.application.port.out.MarketRankingStock;
import seokhoon.trade.domain.stock.Market;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
@ConditionalOnProperty(
        name = "tradeguard.market-data.realtime-provider",
        havingValue = "fake",
        matchIfMissing = true
)
public class FakeMarketRankingAdapter implements MarketRankingPort {
    private static final List<MarketRankingStock> STOCKS = List.of(
            stock("005930", "삼성전자", Market.KOSPI, "75000", "4.2", "52000000000", 8_000_000),
            stock("000660", "SK하이닉스", Market.KOSPI, "180000", "5.1", "88000000000", 3_000_000),
            stock("035420", "NAVER", Market.KOSPI, "220000", "3.8", "41000000000", 900_000),
            stock("051910", "LG화학", Market.KOSPI, "360000", "2.4", "35000000000", 500_000),
            stock("035720", "카카오", Market.KOSPI, "61000", "6.3", "45000000000", 5_500_000),
            stock("247540", "에코프로비엠", Market.KOSDAQ, "210000", "4.6", "61000000000", 2_100_000),
            stock("091990", "셀트리온헬스케어", Market.KOSDAQ, "82000", "3.5", "33000000000", 1_400_000),
            stock("196170", "알테오젠", Market.KOSDAQ, "120000", "7.2", "57000000000", 1_900_000),
            stock("086520", "에코프로", Market.KOSDAQ, "98000", "16.4", "70000000000", 2_500_000)
    );

    @Override
    public List<MarketRankingStock> findTopTradingValueStocks(Market market, int limit) {
        return byMarket(market).stream()
                .sorted(Comparator.comparing(MarketRankingStock::tradingValue).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<MarketRankingStock> findTopRisingStocks(Market market, int limit) {
        return byMarket(market).stream()
                .sorted(Comparator.comparing(MarketRankingStock::changeRate).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<MarketRankingStock> findVolumeSurgeStocks(Market market, int limit) {
        return byMarket(market).stream()
                .sorted(Comparator.comparingLong(MarketRankingStock::volume).reversed())
                .limit(limit)
                .toList();
    }

    private static List<MarketRankingStock> byMarket(Market market) {
        return STOCKS.stream()
                .filter(stock -> stock.market() == market)
                .toList();
    }

    private static MarketRankingStock stock(
            String stockCode,
            String stockName,
            Market market,
            String currentPrice,
            String changeRate,
            String tradingValue,
            long volume
    ) {
        return new MarketRankingStock(
                stockCode,
                stockName,
                market,
                new BigDecimal(currentPrice),
                new BigDecimal(changeRate),
                new BigDecimal(tradingValue),
                volume
        );
    }
}
