package seokhoon.trade.adapter.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import seokhoon.trade.adapter.marketdata.kis.KisProperties;

import static org.assertj.core.api.Assertions.assertThat;

class KisReadOnlyHealthIndicatorTest {
    @Test
    void reportsUpForFakeProviderWithoutCredentials() {
        KisProperties properties = new KisProperties();
        KisReadOnlyHealthIndicator indicator =
                new KisReadOnlyHealthIndicator("fake", properties);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("provider", "fake")
                .containsEntry("readOnly", true)
                .doesNotContainKeys("appKey", "appSecret");
    }

    @Test
    void reportsUnknownForKisProviderWithoutCredentials() {
        KisProperties properties = new KisProperties();
        KisReadOnlyHealthIndicator indicator =
                new KisReadOnlyHealthIndicator("kis", properties);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails())
                .containsEntry("provider", "kis")
                .containsEntry("credentialsConfigured", false)
                .containsEntry("readOnly", true);
    }

    @Test
    void neverExposesConfiguredCredentials() {
        KisProperties properties = new KisProperties();
        properties.setAppKey("sensitive-app-key");
        properties.setAppSecret("sensitive-app-secret");
        KisReadOnlyHealthIndicator indicator =
                new KisReadOnlyHealthIndicator("kis", properties);

        String details = indicator.health().getDetails().toString();

        assertThat(details)
                .doesNotContain("sensitive-app-key")
                .doesNotContain("sensitive-app-secret");
    }
}
