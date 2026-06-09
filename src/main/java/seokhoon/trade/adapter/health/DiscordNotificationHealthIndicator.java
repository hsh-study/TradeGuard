package seokhoon.trade.adapter.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import seokhoon.trade.adapter.notification.discord.DiscordNotificationProperties;

@Component("discordNotification")
public class DiscordNotificationHealthIndicator implements HealthIndicator {
    private final DiscordNotificationProperties properties;

    public DiscordNotificationHealthIndicator(DiscordNotificationProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        boolean enabled = properties.getWebhookUrl() != null
                && !properties.getWebhookUrl().isBlank();
        if (!enabled) {
            return Health.unknown()
                    .withDetail("enabled", false)
                    .build();
        }
        return Health.up()
                .withDetail("enabled", true)
                .build();
    }
}
