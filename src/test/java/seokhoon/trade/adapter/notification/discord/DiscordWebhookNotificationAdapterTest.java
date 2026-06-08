package seokhoon.trade.adapter.notification.discord;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import seokhoon.trade.application.port.out.NotificationMessage;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordWebhookNotificationAdapterTest {
    @Test
    void skipsDeliveryWhenWebhookUrlIsEmpty() {
        DiscordNotificationProperties properties = new DiscordNotificationProperties();
        properties.setWebhookUrl("");
        DiscordWebhookNotificationAdapter adapter = new DiscordWebhookNotificationAdapter(
                properties,
                new ObjectMapper()
        );

        var result = adapter.send(new NotificationMessage(
                "title",
                "body",
                Instant.parse("2026-06-05T06:00:00Z")
        ));

        assertThat(result.sent()).isFalse();
        assertThat(result.message()).isEqualTo("discord webhook url is not configured");
    }
}
