package seokhoon.trade.adapter.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.*;
import org.springframework.stereotype.Component;
import seokhoon.trade.adapter.marketdata.kis.KisProperties;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.config.KisEnvironmentUsage;
import seokhoon.trade.domain.kis.*;

import java.time.*;
import java.util.*;

@Component("kisOAuthToken")
public class KisOAuthTokenHealthIndicator implements HealthIndicator {
    private final KisAccessTokenProvider provider;
    private final KisProperties properties;
    private final KisEnvironmentUsage usage;
    private final Clock clock;

    @Autowired
    public KisOAuthTokenHealthIndicator(
            KisAccessTokenProvider provider,
            KisProperties properties,
            KisEnvironmentUsage usage
    ) {
        this(provider,properties,usage,Clock.systemUTC());
    }

    KisOAuthTokenHealthIndicator(
            KisAccessTokenProvider provider,
            KisProperties properties,
            KisEnvironmentUsage usage,
            Clock clock
    ) {
        this.provider=provider;
        this.properties=properties;
        this.usage=usage;
        this.clock=clock;
    }

    @Override
    public Health health() {
        if (!hasText(properties.getAppKey())
                || !hasText(properties.getAppSecret())) {
            return Health.unknown()
                    .withDetail("credentialsConfigured",false)
                    .build();
        }
        Set<KisEnvironment> enabled=usage.enabledEnvironments();
        if (enabled.isEmpty()) {
            return Health.unknown()
                    .withDetail("providerEnabled",false)
                    .build();
        }
        Instant now=clock.instant();
        boolean down=false;
        boolean unknown=false;
        List<Map<String,Object>> details=new ArrayList<>();
        for (KisEnvironment environment : enabled) {
            Optional<KisAccessToken> optional=
                    provider.findTokenMetadata(environment);
            Map<String,Object> detail=new LinkedHashMap<>();
            detail.put("environment",environment.name());
            detail.put("tokenPresent",optional.isPresent());
            if (optional.isEmpty()) {
                unknown=true;
            } else {
                KisAccessToken token=optional.get();
                long seconds=Duration.between(now,token.expiresAt())
                        .getSeconds();
                detail.put("expiresAt",token.expiresAt());
                detail.put("secondsToExpire",seconds);
                if (seconds <= 0) down=true;
                else if (seconds <= properties.getTokenRefreshBeforeSeconds()) {
                    unknown=true;
                }
            }
            details.add(Map.copyOf(detail));
        }
        Health.Builder builder=down ? Health.down()
                : unknown ? Health.unknown() : Health.up();
        return builder.withDetail("environments",details).build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
