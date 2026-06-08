package seokhoon.trade.adapter.marketdata;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.IntradayMarketSnapshot;
import seokhoon.trade.application.port.out.MarketSnapshotPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(
        name = "tradeguard.market-data.realtime-provider",
        havingValue = "fake",
        matchIfMissing = true
)
public class FakeMarketSnapshotAdapter implements MarketSnapshotPort {
    private static final Map<String, IntradayMarketSnapshot> SNAPSHOTS = Map.of(
            "005930", snapshot("005930", "76000", "4.5", "77000", "72000", 8_500_000, "65000000000", "74500"),
            "000660", snapshot("000660", "181000", "5.4", "183000", "171000", 3_200_000, "92000000000", "178000"),
            "035420", snapshot("035420", "210000", "2.1", "225000", "205000", 950_000, "42000000000", "214000")
    );

    @Override
    public Optional<IntradayMarketSnapshot> getSnapshot(String stockCode) {
        return Optional.ofNullable(SNAPSHOTS.get(stockCode));
    }

    private static IntradayMarketSnapshot snapshot(
            String stockCode,
            String currentPrice,
            String changeRate,
            String intradayHigh,
            String intradayLow,
            long accumulatedVolume,
            String accumulatedTradingValue,
            String vwap
    ) {
        return new IntradayMarketSnapshot(
                stockCode,
                new BigDecimal(currentPrice),
                new BigDecimal(changeRate),
                new BigDecimal(intradayHigh),
                new BigDecimal(intradayLow),
                accumulatedVolume,
                new BigDecimal(accumulatedTradingValue),
                new BigDecimal(vwap),
                Instant.parse("2026-06-05T06:00:00Z")
        );
    }
}
