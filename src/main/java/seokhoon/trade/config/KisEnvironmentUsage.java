package seokhoon.trade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import seokhoon.trade.adapter.marketdata.kis.KisProperties;
import seokhoon.trade.domain.kis.KisEnvironment;

import java.util.EnumSet;
import java.util.Set;

@Component
public class KisEnvironmentUsage {
    private final KisProperties kis;
    private final LiveTradingProperties live;
    private final boolean readOnlyKisEnabled;

    public KisEnvironmentUsage(
            KisProperties kis,
            LiveTradingProperties live,
            @Value("${tradeguard.market-data.realtime-provider:fake}")
            String realtimeProvider,
            @Value("${tradeguard.market-data.intraday-provider:fake}")
            String intradayProvider,
            @Value("${tradeguard.market-data.after-hours-provider:fake}")
            String afterHoursProvider
    ) {
        this.kis=kis;
        this.live=live;
        this.readOnlyKisEnabled=isKis(realtimeProvider)
                || isKis(intradayProvider)
                || isKis(afterHoursProvider);
    }

    public Set<KisEnvironment> enabledEnvironments() {
        EnumSet<KisEnvironment> result=EnumSet.noneOf(KisEnvironment.class);
        if (readOnlyKisEnabled) result.add(kis.getEnvironment());
        if (live.isKisTradingEnabled()) result.add(kis.getEnvironment());
        return Set.copyOf(result);
    }

    public boolean anyEnabled() {
        return !enabledEnvironments().isEmpty();
    }

    private static boolean isKis(String value) {
        return "kis".equalsIgnoreCase(value);
    }
}
