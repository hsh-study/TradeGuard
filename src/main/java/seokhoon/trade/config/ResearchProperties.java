package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tradeguard.research")
public class ResearchProperties {
    private boolean morningNoteDiscordEnabled;

    public boolean isMorningNoteDiscordEnabled() {
        return morningNoteDiscordEnabled;
    }

    public void setMorningNoteDiscordEnabled(boolean morningNoteDiscordEnabled) {
        this.morningNoteDiscordEnabled = morningNoteDiscordEnabled;
    }
}
