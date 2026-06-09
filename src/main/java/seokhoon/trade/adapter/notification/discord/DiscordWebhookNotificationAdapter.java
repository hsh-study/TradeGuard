package seokhoon.trade.adapter.notification.discord;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;

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
    private static final Logger log =
            LoggerFactory.getLogger(DiscordWebhookNotificationAdapter.class);

    private final DiscordNotificationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final OperationalMetricsPort operationalMetricsPort;

    @Autowired
    public DiscordWebhookNotificationAdapter(
            DiscordNotificationProperties properties,
            ObjectMapper objectMapper,
            OperationalMetricsPort operationalMetricsPort
    ) {
        this(properties, objectMapper, HttpClient.newHttpClient(), operationalMetricsPort);
    }

    DiscordWebhookNotificationAdapter(
            DiscordNotificationProperties properties,
            ObjectMapper objectMapper
    ) {
        this(
                properties,
                objectMapper,
                HttpClient.newHttpClient(),
                OperationalMetricsPort.noop()
        );
    }

    DiscordWebhookNotificationAdapter(
            DiscordNotificationProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this(properties, objectMapper, httpClient, OperationalMetricsPort.noop());
    }

    DiscordWebhookNotificationAdapter(
            DiscordNotificationProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            OperationalMetricsPort operationalMetricsPort
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.operationalMetricsPort = operationalMetricsPort;
    }

    @Override
    public NotificationDeliveryResult send(NotificationMessage message) {
        if (!properties.hasWebhookUrl()) {
            return result(
                    NotificationDeliveryResult.skipped("discord webhook url is not configured"),
                    "disabled"
            );
        }

        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of(
                    "content",
                    "**" + message.title() + "**\n" + message.body()
            ));
        } catch (JacksonException exception) {
            return result(
                    NotificationDeliveryResult.skipped("failed to create discord payload"),
                    "failed"
            );
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getWebhookUrl()))
                .timeout(REQUEST_TIMEOUT)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return result(NotificationDeliveryResult.success(), "success");
            }
            return result(
                    NotificationDeliveryResult.skipped(
                            "discord webhook returned HTTP " + response.statusCode()
                    ),
                    "failed"
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return result(
                    NotificationDeliveryResult.skipped("discord webhook request was interrupted"),
                    "failed"
            );
        } catch (IOException | IllegalArgumentException exception) {
            return result(
                    NotificationDeliveryResult.skipped("discord webhook request failed"),
                    "failed"
            );
        }
    }

    private NotificationDeliveryResult result(
            NotificationDeliveryResult deliveryResult,
            String metricResult
    ) {
        operationalMetricsPort.recordDiscordNotification(metricResult);
        log.atInfo()
                .addKeyValue("channel", "discord")
                .addKeyValue("result", metricResult)
                .log("Discord notification completed");
        return deliveryResult;
    }
}
