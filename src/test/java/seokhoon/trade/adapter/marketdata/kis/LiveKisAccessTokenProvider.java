package seokhoon.trade.adapter.marketdata.kis;

import seokhoon.trade.config.LiveTradingProperties;

class LiveKisAccessTokenProvider extends KisAccessTokenProvider {
    private final LiveTradingProperties live;

    LiveKisAccessTokenProvider(
            KisHttpClient client,
            KisProperties kis,
            LiveTradingProperties live
    ) {
        super(client,kis);
        this.live=live;
    }

    String get() {
        return getAccessToken(live.environment());
    }
}
