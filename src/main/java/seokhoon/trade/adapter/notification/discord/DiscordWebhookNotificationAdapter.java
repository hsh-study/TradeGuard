package seokhoon.trade.adapter.notification.discord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class DiscordWebhookNotificationAdapter implements NotificationPort {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final DiscordNotificationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public DiscordWebhookNotificationAdapter(
            DiscordNotificationProperties properties,
            ObjectMapper objectMapper
    ) {
        this(properties, objectMapper, HttpClient.newHttpClient());
    }

    DiscordWebhookNotificationAdapter(
            DiscordNotificationProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public NotificationDeliveryResult send(NotificationMessage message) {
        if (!properties.hasWebhookUrl()) {
            return NotificationDeliveryResult.skipped("discord webhook url is not configured");
        }

        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of(
                    "content",
                    "**" + message.title() + "**\n" + message.body()
            ));
        } catch (JacksonException exception) {
            return NotificationDeliveryResult.skipped("failed to create discord payload");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getWebhookUrl()))
                .timeout(REQUEST_TIMEOUT)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return NotificationDeliveryResult.success();
            }
            return NotificationDeliveryResult.skipped("discord webhook returned HTTP " + response.statusCode());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return NotificationDeliveryResult.skipped("discord webhook request was interrupted");
        } catch (IOException | IllegalArgumentException exception) {
            return NotificationDeliveryResult.skipped("discord webhook request failed");
        }
    }
}
