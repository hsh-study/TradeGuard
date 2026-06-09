package seokhoon.trade.adapter.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import seokhoon.trade.adapter.marketdata.kis.KisProperties;

@Component("kisReadOnly")
public class KisReadOnlyHealthIndicator implements HealthIndicator {
    private final String provider;
    private final KisProperties properties;

    public KisReadOnlyHealthIndicator(
            @Value("${tradeguard.market-data.realtime-provider:fake}") String provider,
            KisProperties properties
    ) {
        this.provider = provider;
        this.properties = properties;
    }

    @Override
    public Health health() {
        if ("fake".equalsIgnoreCase(provider)) {
            return Health.up()
                    .withDetail("provider", "fake")
                    .withDetail("readOnly", true)
                    .build();
        }
        if (!"kis".equalsIgnoreCase(provider)) {
            return Health.unknown()
                    .withDetail("provider", "unsupported")
                    .withDetail("readOnly", true)
                    .build();
        }
        boolean credentialsConfigured = hasText(properties.getAppKey())
                && hasText(properties.getAppSecret());
        if (!credentialsConfigured) {
            return Health.unknown()
                    .withDetail("provider", "kis")
                    .withDetail("credentialsConfigured", false)
                    .withDetail("readOnly", true)
                    .build();
        }
        return Health.up()
                .withDetail("provider", "kis")
                .withDetail("credentialsConfigured", true)
                .withDetail("readOnly", true)
                .build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
