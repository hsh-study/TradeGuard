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
    private final String afterHoursProvider;
    private final KisProperties properties;

    public KisReadOnlyHealthIndicator(
            @Value("${tradeguard.market-data.realtime-provider:fake}")
            String realtimeProvider,
            @Value("${tradeguard.market-data.intraday-provider:fake}")
            String intradayProvider,
            @Value("${tradeguard.market-data.after-hours-provider:fake}")
            String afterHoursProvider,
            @Value("${tradeguard.market-data.after-hours-enabled:true}")
            boolean legacyAfterHoursEnabled,
            KisProperties properties
    ) {
        this.realtimeProvider = realtimeProvider;
        this.intradayProvider = intradayProvider;
        this.afterHoursProvider = normalizedAfterHoursProvider(
                afterHoursProvider,
                legacyAfterHoursEnabled
        );
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (!supported(realtimeProvider)
                || !supported(intradayProvider)
                || !supportedAfterHours(afterHoursProvider)) {
            return base(Health.unknown())
                    .withDetail("provider", "unsupported")
                    .build();
        }
        boolean kisEnabled = "kis".equalsIgnoreCase(realtimeProvider)
                || "kis".equalsIgnoreCase(intradayProvider)
                || "kis".equalsIgnoreCase(afterHoursProvider);
        if (!kisEnabled) {
            return Health.up()
                    .withDetail("provider", "fake")
                    .withDetail("realtimeProvider", realtimeProvider)
                    .withDetail("intradayProvider", intradayProvider)
                    .withDetail("afterHoursProvider", afterHoursProvider)
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
                .withDetail("afterHoursProvider", afterHoursProvider)
                .withDetail("readOnly", true);
    }

    private static boolean supported(String provider) {
        return "fake".equalsIgnoreCase(provider)
                || "kis".equalsIgnoreCase(provider);
    }

    private static boolean supportedAfterHours(String provider) {
        return supported(provider) || "disabled".equalsIgnoreCase(provider);
    }

    private static String normalizedAfterHoursProvider(
            String provider,
            boolean legacyEnabled
    ) {
        return provider == null || provider.isBlank()
                ? legacyEnabled ? "fake" : "disabled"
                : provider;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
