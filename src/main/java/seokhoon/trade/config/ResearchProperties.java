package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tradeguard.research")
public class ResearchProperties {
    private boolean morningNoteDiscordEnabled;
    private boolean earningsEventAutoCreateCatalyst = true;

    public boolean isMorningNoteDiscordEnabled() {
        return morningNoteDiscordEnabled;
    }

    public void setMorningNoteDiscordEnabled(boolean morningNoteDiscordEnabled) {
        this.morningNoteDiscordEnabled = morningNoteDiscordEnabled;
    }

    public boolean isEarningsEventAutoCreateCatalyst() {
        return earningsEventAutoCreateCatalyst;
    }

    public void setEarningsEventAutoCreateCatalyst(boolean earningsEventAutoCreateCatalyst) {
        this.earningsEventAutoCreateCatalyst = earningsEventAutoCreateCatalyst;
    }
}
