package seokhoon.trade.application.port.in;

import java.math.BigDecimal;

public record EarlyMarketStrategyParameterOverrides(
        PreOpen preOpen,
        Opening opening,
        FollowUp followUp,
        PriceAction priceAction
) {
    public record PreOpen(
            BigDecimal afterHoursRiseThreshold,
            Integer afterHoursRiseScore,
            BigDecimal afterHoursTradingValueThreshold,
            Integer afterHoursTradingValueScore,
            BigDecimal afterHoursOverheatThreshold,
            Integer afterHoursOverheatPenalty,
            BigDecimal afterHoursFallThreshold,
            Integer afterHoursFallPenalty
    ) {
    }

    public record Opening(
            Integer vwapAboveScore,
            Integer nearHighScore,
            Integer tradingValueScore,
            Integer vwapBrokenPenalty,
            Integer highDrawdownPenalty,
            Integer entryThreshold,
            Integer maxCandidates
    ) {
    }

    public record FollowUp(
            BigDecimal excludeDrawdownFromHigh,
            BigDecimal cautionDrawdownFromHigh,
            Boolean excludeWhenLastBelowVwap,
            Boolean excludeWhenLastBelowOpeningPrice,
            Boolean cautionWhenPreviousHighNotBroken,
            Boolean cautionWhenPreviousHighReLost
    ) {
    }

    public record PriceAction(
            Integer previousHighBreakoutScore,
            Integer previousHighNotBrokenPenalty,
            Integer openingPriceHeldScore,
            Integer openingPriceLostPenalty
    ) {
    }
}
