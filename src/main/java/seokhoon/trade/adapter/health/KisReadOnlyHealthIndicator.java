package seokhoon.trade.adapter.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import seokhoon.trade.adapter.marketdata.kis.KisProperties;

@Component("kisReadOnly")
public class KisReadOnlyHealthIndicator implements HealthIndicator {
    private final String realtimeProvider;
    private final String intradayProvider;
    private final KisProperties properties;

    public KisReadOnlyHealthIndicator(
            @Value("${tradeguard.market-data.realtime-provider:fake}")
            String realtimeProvider,
            @Value("${tradeguard.market-data.intraday-provider:fake}")
            String intradayProvider,
            KisProperties properties
    ) {
        this.realtimeProvider = realtimeProvider;
        this.intradayProvider = intradayProvider;
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (!supported(realtimeProvider) || !supported(intradayProvider)) {
            return base(Health.unknown())
                    .withDetail("provider", "unsupported")
                    .build();
        }
        boolean kisEnabled = "kis".equalsIgnoreCase(realtimeProvider)
                || "kis".equalsIgnoreCase(intradayProvider);
        if (!kisEnabled) {
            return Health.up()
                    .withDetail("provider", "fake")
                    .withDetail("realtimeProvider", realtimeProvider)
                    .withDetail("intradayProvider", intradayProvider)
                    .withDetail("readOnly", true)
                    .build();
        }
        boolean credentialsConfigured = hasText(properties.getAppKey())
                && hasText(properties.getAppSecret());
        if (!credentialsConfigured) {
            return base(Health.unknown())
                    .withDetail("provider", "kis")
                    .withDetail("credentialsConfigured", false)
                    .build();
        }
        return base(Health.up())
                .withDetail("provider", "kis")
                .withDetail("credentialsConfigured", true)
                .build();
    }

    private Health.Builder base(Health.Builder builder) {
        return builder
                .withDetail("realtimeProvider", realtimeProvider)
                .withDetail("intradayProvider", intradayProvider)
                .withDetail("readOnly", true);
    }

    private static boolean supported(String provider) {
        return "fake".equalsIgnoreCase(provider)
                || "kis".equalsIgnoreCase(provider);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
