package seokhoon.trade.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Component
@Validated
@ConfigurationProperties(prefix = "tradeguard.early-market.strategy")
public class EarlyMarketStrategyProperties {
    @Valid
    private final PreOpen preOpen = new PreOpen();
    @Valid
    private final Opening opening = new Opening();
    @Valid
    private final FollowUp followUp = new FollowUp();
    @Valid
    private final PriceAction priceAction = new PriceAction();

    public PreOpen getPreOpen() {
        return preOpen;
    }

    public Opening getOpening() {
        return opening;
    }

    public FollowUp getFollowUp() {
        return followUp;
    }

    public PriceAction getPriceAction() {
        return priceAction;
    }

    public static class PreOpen {
        @NotNull
        @DecimalMin("0.0")
        private BigDecimal afterHoursRiseThreshold = new BigDecimal("3.0");
        @Min(0)
        private int afterHoursRiseScore = 15;
        @NotNull
        @DecimalMin("0")
        private BigDecimal afterHoursTradingValueThreshold =
                new BigDecimal("30000000000");
        @Min(0)
        private int afterHoursTradingValueScore = 15;
        @NotNull
        @DecimalMin("0.0")
        private BigDecimal afterHoursOverheatThreshold = new BigDecimal("7.0");
        @Max(0)
        private int afterHoursOverheatPenalty = -10;
        @NotNull
        @DecimalMax("0.0")
        private BigDecimal afterHoursFallThreshold = new BigDecimal("-3.0");
        @Max(0)
        private int afterHoursFallPenalty = -10;

        public BigDecimal getAfterHoursRiseThreshold() {
            return afterHoursRiseThreshold;
        }

        public void setAfterHoursRiseThreshold(BigDecimal value) {
            this.afterHoursRiseThreshold = value;
        }

        public int getAfterHoursRiseScore() {
            return afterHoursRiseScore;
        }

        public void setAfterHoursRiseScore(int value) {
            this.afterHoursRiseScore = value;
        }

        public BigDecimal getAfterHoursTradingValueThreshold() {
            return afterHoursTradingValueThreshold;
        }

        public void setAfterHoursTradingValueThreshold(BigDecimal value) {
            this.afterHoursTradingValueThreshold = value;
        }

        public int getAfterHoursTradingValueScore() {
            return afterHoursTradingValueScore;
        }

        public void setAfterHoursTradingValueScore(int value) {
            this.afterHoursTradingValueScore = value;
        }

        public BigDecimal getAfterHoursOverheatThreshold() {
            return afterHoursOverheatThreshold;
        }

        public void setAfterHoursOverheatThreshold(BigDecimal value) {
            this.afterHoursOverheatThreshold = value;
        }

        public int getAfterHoursOverheatPenalty() {
            return afterHoursOverheatPenalty;
        }

        public void setAfterHoursOverheatPenalty(int value) {
            this.afterHoursOverheatPenalty = value;
        }

        public BigDecimal getAfterHoursFallThreshold() {
            return afterHoursFallThreshold;
        }

        public void setAfterHoursFallThreshold(BigDecimal value) {
            this.afterHoursFallThreshold = value;
        }

        public int getAfterHoursFallPenalty() {
            return afterHoursFallPenalty;
        }

        public void setAfterHoursFallPenalty(int value) {
            this.afterHoursFallPenalty = value;
        }

        @AssertTrue(message = "after-hours-overheat-threshold must be greater than or equal to after-hours-rise-threshold")
        public boolean isAfterHoursThresholdOrderValid() {
            return afterHoursRiseThreshold == null
                    || afterHoursOverheatThreshold == null
                    || afterHoursOverheatThreshold.compareTo(afterHoursRiseThreshold) >= 0;
        }
    }

    public static class Opening {
        @Min(0)
        private int vwapAboveScore = 25;
        @Min(0)
        private int nearHighScore = 20;
        @Min(0)
        private int tradingValueScore = 20;
        @Max(0)
        private int vwapBrokenPenalty = -30;
        @Max(0)
        private int highDrawdownPenalty = -20;
        @Min(0)
        @Max(100)
        private int entryThreshold = 70;
        @Min(1)
        private int maxCandidates = 3;

        public int getVwapAboveScore() {
            return vwapAboveScore;
        }

        public void setVwapAboveScore(int value) {
            this.vwapAboveScore = value;
        }

        public int getNearHighScore() {
            return nearHighScore;
        }

        public void setNearHighScore(int value) {
            this.nearHighScore = value;
        }

        public int getTradingValueScore() {
            return tradingValueScore;
        }

        public void setTradingValueScore(int value) {
            this.tradingValueScore = value;
        }

        public int getVwapBrokenPenalty() {
            return vwapBrokenPenalty;
        }

        public void setVwapBrokenPenalty(int value) {
            this.vwapBrokenPenalty = value;
        }

        public int getHighDrawdownPenalty() {
            return highDrawdownPenalty;
        }

        public void setHighDrawdownPenalty(int value) {
            this.highDrawdownPenalty = value;
        }

        public int getEntryThreshold() {
            return entryThreshold;
        }

        public void setEntryThreshold(int value) {
            this.entryThreshold = value;
        }

        public int getMaxCandidates() {
            return maxCandidates;
        }

        public void setMaxCandidates(int value) {
            this.maxCandidates = value;
        }
    }

    public static class FollowUp {
        @NotNull
        @DecimalMax("0.0")
        private BigDecimal excludeDrawdownFromHigh = new BigDecimal("-2.0");
        @NotNull
        @DecimalMax("0.0")
        private BigDecimal cautionDrawdownFromHigh = new BigDecimal("-1.0");
        private boolean excludeWhenLastBelowVwap = true;
        private boolean excludeWhenLastBelowOpeningPrice = true;
        private boolean cautionWhenPreviousHighNotBroken = true;
        private boolean cautionWhenPreviousHighReLost = true;

        public BigDecimal getExcludeDrawdownFromHigh() {
            return excludeDrawdownFromHigh;
        }

        public void setExcludeDrawdownFromHigh(BigDecimal value) {
            this.excludeDrawdownFromHigh = value;
        }

        public BigDecimal getCautionDrawdownFromHigh() {
            return cautionDrawdownFromHigh;
        }

        public void setCautionDrawdownFromHigh(BigDecimal value) {
            this.cautionDrawdownFromHigh = value;
        }

        public boolean isExcludeWhenLastBelowVwap() {
            return excludeWhenLastBelowVwap;
        }

        public void setExcludeWhenLastBelowVwap(boolean value) {
            this.excludeWhenLastBelowVwap = value;
        }

        public boolean isExcludeWhenLastBelowOpeningPrice() {
            return excludeWhenLastBelowOpeningPrice;
        }

        public void setExcludeWhenLastBelowOpeningPrice(boolean value) {
            this.excludeWhenLastBelowOpeningPrice = value;
        }

        public boolean isCautionWhenPreviousHighNotBroken() {
            return cautionWhenPreviousHighNotBroken;
        }

        public void setCautionWhenPreviousHighNotBroken(boolean value) {
            this.cautionWhenPreviousHighNotBroken = value;
        }

        public boolean isCautionWhenPreviousHighReLost() {
            return cautionWhenPreviousHighReLost;
        }

        public void setCautionWhenPreviousHighReLost(boolean value) {
            this.cautionWhenPreviousHighReLost = value;
        }

        @AssertTrue(message = "exclude-drawdown-from-high must be less than or equal to caution-drawdown-from-high")
        public boolean isDrawdownOrderValid() {
            return excludeDrawdownFromHigh == null
                    || cautionDrawdownFromHigh == null
                    || excludeDrawdownFromHigh.compareTo(cautionDrawdownFromHigh) <= 0;
        }
    }

    public static class PriceAction {
        @Min(0)
        private int previousHighBreakoutScore = 15;
        @Max(0)
        private int previousHighNotBrokenPenalty = -10;
        @Min(0)
        private int openingPriceHeldScore = 10;
        @Max(0)
        private int openingPriceLostPenalty = -15;

        public int getPreviousHighBreakoutScore() {
            return previousHighBreakoutScore;
        }

        public void setPreviousHighBreakoutScore(int value) {
            this.previousHighBreakoutScore = value;
        }

        public int getPreviousHighNotBrokenPenalty() {
            return previousHighNotBrokenPenalty;
        }

        public void setPreviousHighNotBrokenPenalty(int value) {
            this.previousHighNotBrokenPenalty = value;
        }

        public int getOpeningPriceHeldScore() {
            return openingPriceHeldScore;
        }

        public void setOpeningPriceHeldScore(int value) {
            this.openingPriceHeldScore = value;
        }

        public int getOpeningPriceLostPenalty() {
            return openingPriceLostPenalty;
        }

        public void setOpeningPriceLostPenalty(int value) {
            this.openingPriceLostPenalty = value;
        }

    }
}
