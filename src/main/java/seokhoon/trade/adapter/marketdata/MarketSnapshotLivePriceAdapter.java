package seokhoon.trade.adapter.marketdata;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import seokhoon.trade.application.port.out.LivePricePort;
import seokhoon.trade.application.port.out.MarketSnapshotPort;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(
        name = "tradeguard.live-trading.kis-trading-enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class MarketSnapshotLivePriceAdapter implements LivePricePort {
    private final MarketSnapshotPort marketSnapshotPort;

    public MarketSnapshotLivePriceAdapter(MarketSnapshotPort marketSnapshotPort) {
        this.marketSnapshotPort = marketSnapshotPort;
    }

    @Override
    public BigDecimal getCurrentPrice(String stockCode) {
        return marketSnapshotPort.getSnapshot(stockCode)
                .orElseThrow(() -> new IllegalStateException("Current price is unavailable"))
                .currentPrice();
    }
}
