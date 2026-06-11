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
                new KisReadOnlyHealthIndicator(
                        "fake",
                        "fake",
                        "fake",
                        true,
                        properties
                );

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("provider", "fake")
                .containsEntry("realtimeProvider", "fake")
                .containsEntry("intradayProvider", "fake")
                .containsEntry("afterHoursProvider", "fake")
                .containsEntry("readOnly", true)
                .doesNotContainKeys("appKey", "appSecret");
    }

    @Test
    void reportsUnknownForKisProviderWithoutCredentials() {
        KisProperties properties = new KisProperties();
        KisReadOnlyHealthIndicator indicator =
                new KisReadOnlyHealthIndicator(
                        "kis",
                        "fake",
                        "fake",
                        true,
                        properties
                );

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails())
                .containsEntry("provider", "kis")
                .containsEntry("realtimeProvider", "kis")
                .containsEntry("intradayProvider", "fake")
                .containsEntry("credentialsConfigured", false)
                .containsEntry("readOnly", true);
    }

    @Test
    void reportsUnknownForKisIntradayProviderWithoutCredentials() {
        KisReadOnlyHealthIndicator indicator = new KisReadOnlyHealthIndicator(
                "fake",
                "kis",
                "fake",
                true,
                new KisProperties()
        );

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(indicator.health().getDetails())
                .containsEntry("realtimeProvider", "fake")
                .containsEntry("intradayProvider", "kis")
                .containsEntry("credentialsConfigured", false);
    }

    @Test
    void reportsUnknownForKisAfterHoursProviderWithoutCredentials() {
        KisReadOnlyHealthIndicator indicator = new KisReadOnlyHealthIndicator(
                "fake",
                "fake",
                "kis",
                true,
                new KisProperties()
        );

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(indicator.health().getDetails())
                .containsEntry("afterHoursProvider", "kis")
                .containsEntry("credentialsConfigured", false);
    }

    @Test
    void reportsDisabledForLegacyAfterHoursFlag() {
        KisReadOnlyHealthIndicator indicator = new KisReadOnlyHealthIndicator(
                "fake",
                "fake",
                "",
                false,
                new KisProperties()
        );

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(indicator.health().getDetails())
                .containsEntry("afterHoursProvider", "disabled");
    }

    @Test
    void neverExposesConfiguredCredentials() {
        KisProperties properties = new KisProperties();
        properties.setAppKey("sensitive-app-key");
        properties.setAppSecret("sensitive-app-secret");
        KisReadOnlyHealthIndicator indicator =
                new KisReadOnlyHealthIndicator(
                        "kis",
                        "kis",
                        "kis",
                        true,
                        properties
                );

        String details = indicator.health().getDetails().toString();

        assertThat(details)
                .doesNotContain("sensitive-app-key")
                .doesNotContain("sensitive-app-secret");
    }
}
