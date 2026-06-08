package seokhoon.trade.adapter.notification.discord;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tradeguard.notification.discord")
public class DiscordNotificationProperties {
    private String webhookUrl = "";

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    boolean hasWebhookUrl() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }
}
