package seokhoon.trade.application.port.out;

import java.util.Optional;

public interface MarketSnapshotPort {
    Optional<IntradayMarketSnapshot> getSnapshot(String stockCode);
}
