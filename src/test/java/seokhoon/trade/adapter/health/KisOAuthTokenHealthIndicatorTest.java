package seokhoon.trade.adapter.health;

import org.junit.jupiter.api.Test;
import seokhoon.trade.adapter.marketdata.kis.KisProperties;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.config.KisEnvironmentUsage;
import seokhoon.trade.domain.kis.*;

import java.time.*;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KisOAuthTokenHealthIndicatorTest {
    private static final Instant NOW=Instant.parse("2026-06-13T00:00:00Z");

    @Test
    void reportsUpWithoutExposingToken() {
        var provider=mock(KisAccessTokenProvider.class);
        var usage=mock(KisEnvironmentUsage.class);
        var properties=properties();
        when(usage.enabledEnvironments()).thenReturn(
                Set.of(KisEnvironment.REAL));
        when(provider.findTokenMetadata(KisEnvironment.REAL))
                .thenReturn(Optional.of(new KisAccessToken(
                        KisEnvironment.REAL,"raw-secret-token","Bearer",
                        NOW.plusSeconds(3600),NOW,"safe-id")));
        var health=new KisOAuthTokenHealthIndicator(provider,properties,usage,
                Clock.fixed(NOW,ZoneOffset.UTC)).health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails().toString())
                .doesNotContain("raw-secret-token")
                .doesNotContain("safe-id");
    }

    @Test
    void reportsUnknownWhenTokenIsMissingAndDownWhenExpired() {
        var provider=mock(KisAccessTokenProvider.class);
        var usage=mock(KisEnvironmentUsage.class);
        when(usage.enabledEnvironments()).thenReturn(
                Set.of(KisEnvironment.DEMO));
        var indicator=new KisOAuthTokenHealthIndicator(provider,properties(),
                usage,Clock.fixed(NOW,ZoneOffset.UTC));
        when(provider.findTokenMetadata(KisEnvironment.DEMO))
                .thenReturn(Optional.empty());
        assertThat(indicator.health().getStatus().getCode())
                .isEqualTo("UNKNOWN");

        when(provider.findTokenMetadata(KisEnvironment.DEMO))
                .thenReturn(Optional.of(new KisAccessToken(
                        KisEnvironment.DEMO,"expired","Bearer",
                        NOW.minusSeconds(1),NOW.minusSeconds(3600),"id")));
        assertThat(indicator.health().getStatus().getCode())
                .isEqualTo("DOWN");
    }

    private static KisProperties properties() {
        KisProperties properties=new KisProperties();
        properties.setAppKey("key");
        properties.setAppSecret("secret");
        return properties;
    }
}
