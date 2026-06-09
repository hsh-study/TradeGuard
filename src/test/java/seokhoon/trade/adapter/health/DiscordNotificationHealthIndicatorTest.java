package seokhoon.trade.adapter.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import seokhoon.trade.adapter.notification.discord.DiscordNotificationProperties;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordNotificationHealthIndicatorTest {
    @Test
    void reportsUnknownWhenWebhookIsNotConfigured() {
        DiscordNotificationProperties properties = new DiscordNotificationProperties();
        DiscordNotificationHealthIndicator indicator =
                new DiscordNotificationHealthIndicator(properties);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("enabled", false);
    }

    @Test
    void reportsEnabledWithoutExposingOrCallingWebhook() {
        DiscordNotificationProperties properties = new DiscordNotificationProperties();
        properties.setWebhookUrl("https://discord.example/sensitive-webhook");
        DiscordNotificationHealthIndicator indicator =
                new DiscordNotificationHealthIndicator(properties);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("enabled", true);
        assertThat(health.getDetails().toString())
                .doesNotContain("sensitive-webhook")
                .doesNotContain("discord.example");
    }
}
