package seokhoon.trade.adapter.notification.discord;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import seokhoon.trade.adapter.metrics.MicrometerOperationalMetricsAdapter;
import tools.jackson.databind.ObjectMapper;
import seokhoon.trade.application.port.out.NotificationMessage;

import java.net.http.HttpClient;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordWebhookNotificationAdapterTest {
    @Test
    void skipsDeliveryWhenWebhookUrlIsEmpty() {
        DiscordNotificationProperties properties = new DiscordNotificationProperties();
        properties.setWebhookUrl("");
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        DiscordWebhookNotificationAdapter adapter = new DiscordWebhookNotificationAdapter(
                properties,
                new ObjectMapper(),
                HttpClient.newHttpClient(),
                new MicrometerOperationalMetricsAdapter(registry)
        );

        var result = adapter.send(new NotificationMessage(
                "title",
                "body",
                Instant.parse("2026-06-05T06:00:00Z")
        ));

        assertThat(result.sent()).isFalse();
        assertThat(result.message()).isEqualTo("discord webhook url is not configured");
        assertThat(registry.find("tradeguard.notification.discord.count")
                .tag("result", "disabled")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .noneMatch(tag -> tag.getValue().contains("webhook"));
    }
}
