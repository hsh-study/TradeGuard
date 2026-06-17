package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tradeguard.research")
public class ResearchProperties {
    private boolean morningNoteDiscordEnabled;
    private boolean earningsEventAutoCreateCatalyst = true;
    private boolean valuationAutoSnapshotEnabled = true;
    private int valuationAutoSnapshotLookbackDays = 30;
    private boolean valuationAutoSnapshotRequireSharesOutstanding = true;
    private boolean valuationAutoSnapshotAutoAnalyze = true;
    private boolean sharesOutstandingImportAutoGenerateValuation = false;

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

    public boolean isValuationAutoSnapshotEnabled() {
        return valuationAutoSnapshotEnabled;
    }

    public void setValuationAutoSnapshotEnabled(boolean valuationAutoSnapshotEnabled) {
        this.valuationAutoSnapshotEnabled = valuationAutoSnapshotEnabled;
    }

    public int getValuationAutoSnapshotLookbackDays() {
        return valuationAutoSnapshotLookbackDays;
    }

    public void setValuationAutoSnapshotLookbackDays(int valuationAutoSnapshotLookbackDays) {
        this.valuationAutoSnapshotLookbackDays = valuationAutoSnapshotLookbackDays;
    }

    public boolean isValuationAutoSnapshotRequireSharesOutstanding() {
        return valuationAutoSnapshotRequireSharesOutstanding;
    }

    public void setValuationAutoSnapshotRequireSharesOutstanding(boolean valuationAutoSnapshotRequireSharesOutstanding) {
        this.valuationAutoSnapshotRequireSharesOutstanding = valuationAutoSnapshotRequireSharesOutstanding;
    }

    public boolean isValuationAutoSnapshotAutoAnalyze() {
        return valuationAutoSnapshotAutoAnalyze;
    }

    public void setValuationAutoSnapshotAutoAnalyze(boolean valuationAutoSnapshotAutoAnalyze) {
        this.valuationAutoSnapshotAutoAnalyze = valuationAutoSnapshotAutoAnalyze;
    }

    public boolean isSharesOutstandingImportAutoGenerateValuation() {
        return sharesOutstandingImportAutoGenerateValuation;
    }

    public void setSharesOutstandingImportAutoGenerateValuation(boolean sharesOutstandingImportAutoGenerateValuation) {
        this.sharesOutstandingImportAutoGenerateValuation = sharesOutstandingImportAutoGenerateValuation;
    }
}
