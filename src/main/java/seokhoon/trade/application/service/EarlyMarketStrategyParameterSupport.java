package seokhoon.trade.application.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.EarlyMarketStrategyParameterOverrides;
import seokhoon.trade.config.EarlyMarketStrategyProperties;

import java.util.Map;
import java.util.Set;

@Component
public class EarlyMarketStrategyParameterSupport {
    private final Validator validator;

    public EarlyMarketStrategyParameterSupport(Validator validator) {
        this.validator = validator;
    }

    public EarlyMarketStrategyProperties copyAndApply(
            EarlyMarketStrategyProperties source,
            EarlyMarketStrategyParameterOverrides overrides
    ) {
        EarlyMarketStrategyProperties copy = copy(source);
        if (overrides != null) {
            apply(copy, overrides);
        }
        Set<ConstraintViolation<EarlyMarketStrategyProperties>> violations =
                validator.validate(copy);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return copy;
    }

    public static Map<String, Object> snapshot(
            EarlyMarketStrategyProperties properties
    ) {
        EarlyMarketStrategyProperties.PreOpen preOpen =
                properties.getPreOpen();
        EarlyMarketStrategyProperties.Opening opening =
                properties.getOpening();
        EarlyMarketStrategyProperties.FollowUp followUp =
                properties.getFollowUp();
        EarlyMarketStrategyProperties.PriceAction priceAction =
                properties.getPriceAction();
        return Map.of(
                "preOpen", Map.of(
                        "afterHoursRiseThreshold",
                        preOpen.getAfterHoursRiseThreshold(),
                        "afterHoursRiseScore",
                        preOpen.getAfterHoursRiseScore(),
                        "afterHoursTradingValueThreshold",
                        preOpen.getAfterHoursTradingValueThreshold(),
                        "afterHoursTradingValueScore",
                        preOpen.getAfterHoursTradingValueScore(),
                        "afterHoursOverheatThreshold",
                        preOpen.getAfterHoursOverheatThreshold(),
                        "afterHoursOverheatPenalty",
                        preOpen.getAfterHoursOverheatPenalty(),
                        "afterHoursFallThreshold",
                        preOpen.getAfterHoursFallThreshold(),
                        "afterHoursFallPenalty",
                        preOpen.getAfterHoursFallPenalty()
                ),
                "opening", Map.of(
                        "vwapAboveScore", opening.getVwapAboveScore(),
                        "nearHighScore", opening.getNearHighScore(),
                        "tradingValueScore", opening.getTradingValueScore(),
                        "vwapBrokenPenalty", opening.getVwapBrokenPenalty(),
                        "highDrawdownPenalty", opening.getHighDrawdownPenalty(),
                        "entryThreshold", opening.getEntryThreshold(),
                        "maxCandidates", opening.getMaxCandidates()
                ),
                "followUp", Map.of(
                        "excludeDrawdownFromHigh",
                        followUp.getExcludeDrawdownFromHigh(),
                        "cautionDrawdownFromHigh",
                        followUp.getCautionDrawdownFromHigh(),
                        "excludeWhenLastBelowVwap",
                        followUp.isExcludeWhenLastBelowVwap(),
                        "excludeWhenLastBelowOpeningPrice",
                        followUp.isExcludeWhenLastBelowOpeningPrice(),
                        "cautionWhenPreviousHighNotBroken",
                        followUp.isCautionWhenPreviousHighNotBroken(),
                        "cautionWhenPreviousHighReLost",
                        followUp.isCautionWhenPreviousHighReLost()
                ),
                "priceAction", Map.of(
                        "previousHighBreakoutScore",
                        priceAction.getPreviousHighBreakoutScore(),
                        "previousHighNotBrokenPenalty",
                        priceAction.getPreviousHighNotBrokenPenalty(),
                        "openingPriceHeldScore",
                        priceAction.getOpeningPriceHeldScore(),
                        "openingPriceLostPenalty",
                        priceAction.getOpeningPriceLostPenalty()
                )
        );
    }

    private static EarlyMarketStrategyProperties copy(
            EarlyMarketStrategyProperties source
    ) {
        EarlyMarketStrategyProperties copy =
                new EarlyMarketStrategyProperties();
        copyPreOpen(source.getPreOpen(), copy.getPreOpen());
        copyOpening(source.getOpening(), copy.getOpening());
        copyFollowUp(source.getFollowUp(), copy.getFollowUp());
        copyPriceAction(source.getPriceAction(), copy.getPriceAction());
        return copy;
    }

    private static void apply(
            EarlyMarketStrategyProperties target,
            EarlyMarketStrategyParameterOverrides overrides
    ) {
        applyPreOpen(target.getPreOpen(), overrides.preOpen());
        applyOpening(target.getOpening(), overrides.opening());
        applyFollowUp(target.getFollowUp(), overrides.followUp());
        applyPriceAction(target.getPriceAction(), overrides.priceAction());
    }

    private static void copyPreOpen(
            EarlyMarketStrategyProperties.PreOpen source,
            EarlyMarketStrategyProperties.PreOpen target
    ) {
        target.setAfterHoursRiseThreshold(source.getAfterHoursRiseThreshold());
        target.setAfterHoursRiseScore(source.getAfterHoursRiseScore());
        target.setAfterHoursTradingValueThreshold(
                source.getAfterHoursTradingValueThreshold()
        );
        target.setAfterHoursTradingValueScore(
                source.getAfterHoursTradingValueScore()
        );
        target.setAfterHoursOverheatThreshold(
                source.getAfterHoursOverheatThreshold()
        );
        target.setAfterHoursOverheatPenalty(
                source.getAfterHoursOverheatPenalty()
        );
        target.setAfterHoursFallThreshold(source.getAfterHoursFallThreshold());
        target.setAfterHoursFallPenalty(source.getAfterHoursFallPenalty());
    }

    private static void copyOpening(
            EarlyMarketStrategyProperties.Opening source,
            EarlyMarketStrategyProperties.Opening target
    ) {
        target.setVwapAboveScore(source.getVwapAboveScore());
        target.setNearHighScore(source.getNearHighScore());
        target.setTradingValueScore(source.getTradingValueScore());
        target.setVwapBrokenPenalty(source.getVwapBrokenPenalty());
        target.setHighDrawdownPenalty(source.getHighDrawdownPenalty());
        target.setEntryThreshold(source.getEntryThreshold());
        target.setMaxCandidates(source.getMaxCandidates());
    }

    private static void copyFollowUp(
            EarlyMarketStrategyProperties.FollowUp source,
            EarlyMarketStrategyProperties.FollowUp target
    ) {
        target.setExcludeDrawdownFromHigh(
                source.getExcludeDrawdownFromHigh()
        );
        target.setCautionDrawdownFromHigh(
                source.getCautionDrawdownFromHigh()
        );
        target.setExcludeWhenLastBelowVwap(
                source.isExcludeWhenLastBelowVwap()
        );
        target.setExcludeWhenLastBelowOpeningPrice(
                source.isExcludeWhenLastBelowOpeningPrice()
        );
        target.setCautionWhenPreviousHighNotBroken(
                source.isCautionWhenPreviousHighNotBroken()
        );
        target.setCautionWhenPreviousHighReLost(
                source.isCautionWhenPreviousHighReLost()
        );
    }

    private static void copyPriceAction(
            EarlyMarketStrategyProperties.PriceAction source,
            EarlyMarketStrategyProperties.PriceAction target
    ) {
        target.setPreviousHighBreakoutScore(
                source.getPreviousHighBreakoutScore()
        );
        target.setPreviousHighNotBrokenPenalty(
                source.getPreviousHighNotBrokenPenalty()
        );
        target.setOpeningPriceHeldScore(source.getOpeningPriceHeldScore());
        target.setOpeningPriceLostPenalty(source.getOpeningPriceLostPenalty());
    }

    private static void applyPreOpen(
            EarlyMarketStrategyProperties.PreOpen target,
            EarlyMarketStrategyParameterOverrides.PreOpen overrides
    ) {
        if (overrides == null) {
            return;
        }
        if (overrides.afterHoursRiseThreshold() != null) {
            target.setAfterHoursRiseThreshold(
                    overrides.afterHoursRiseThreshold()
            );
        }
        if (overrides.afterHoursRiseScore() != null) {
            target.setAfterHoursRiseScore(overrides.afterHoursRiseScore());
        }
        if (overrides.afterHoursTradingValueThreshold() != null) {
            target.setAfterHoursTradingValueThreshold(
                    overrides.afterHoursTradingValueThreshold()
            );
        }
        if (overrides.afterHoursTradingValueScore() != null) {
            target.setAfterHoursTradingValueScore(
                    overrides.afterHoursTradingValueScore()
            );
        }
        if (overrides.afterHoursOverheatThreshold() != null) {
            target.setAfterHoursOverheatThreshold(
                    overrides.afterHoursOverheatThreshold()
            );
        }
        if (overrides.afterHoursOverheatPenalty() != null) {
            target.setAfterHoursOverheatPenalty(
                    overrides.afterHoursOverheatPenalty()
            );
        }
        if (overrides.afterHoursFallThreshold() != null) {
            target.setAfterHoursFallThreshold(
                    overrides.afterHoursFallThreshold()
            );
        }
        if (overrides.afterHoursFallPenalty() != null) {
            target.setAfterHoursFallPenalty(
                    overrides.afterHoursFallPenalty()
            );
        }
    }

    private static void applyOpening(
            EarlyMarketStrategyProperties.Opening target,
            EarlyMarketStrategyParameterOverrides.Opening overrides
    ) {
        if (overrides == null) {
            return;
        }
        if (overrides.vwapAboveScore() != null) {
            target.setVwapAboveScore(overrides.vwapAboveScore());
        }
        if (overrides.nearHighScore() != null) {
            target.setNearHighScore(overrides.nearHighScore());
        }
        if (overrides.tradingValueScore() != null) {
            target.setTradingValueScore(overrides.tradingValueScore());
        }
        if (overrides.vwapBrokenPenalty() != null) {
            target.setVwapBrokenPenalty(overrides.vwapBrokenPenalty());
        }
        if (overrides.highDrawdownPenalty() != null) {
            target.setHighDrawdownPenalty(overrides.highDrawdownPenalty());
        }
        if (overrides.entryThreshold() != null) {
            target.setEntryThreshold(overrides.entryThreshold());
        }
        if (overrides.maxCandidates() != null) {
            target.setMaxCandidates(overrides.maxCandidates());
        }
    }

    private static void applyFollowUp(
            EarlyMarketStrategyProperties.FollowUp target,
            EarlyMarketStrategyParameterOverrides.FollowUp overrides
    ) {
        if (overrides == null) {
            return;
        }
        if (overrides.excludeDrawdownFromHigh() != null) {
            target.setExcludeDrawdownFromHigh(
                    overrides.excludeDrawdownFromHigh()
            );
        }
        if (overrides.cautionDrawdownFromHigh() != null) {
            target.setCautionDrawdownFromHigh(
                    overrides.cautionDrawdownFromHigh()
            );
        }
        if (overrides.excludeWhenLastBelowVwap() != null) {
            target.setExcludeWhenLastBelowVwap(
                    overrides.excludeWhenLastBelowVwap()
            );
        }
        if (overrides.excludeWhenLastBelowOpeningPrice() != null) {
            target.setExcludeWhenLastBelowOpeningPrice(
                    overrides.excludeWhenLastBelowOpeningPrice()
            );
        }
        if (overrides.cautionWhenPreviousHighNotBroken() != null) {
            target.setCautionWhenPreviousHighNotBroken(
                    overrides.cautionWhenPreviousHighNotBroken()
            );
        }
        if (overrides.cautionWhenPreviousHighReLost() != null) {
            target.setCautionWhenPreviousHighReLost(
                    overrides.cautionWhenPreviousHighReLost()
            );
        }
    }

    private static void applyPriceAction(
            EarlyMarketStrategyProperties.PriceAction target,
            EarlyMarketStrategyParameterOverrides.PriceAction overrides
    ) {
        if (overrides == null) {
            return;
        }
        if (overrides.previousHighBreakoutScore() != null) {
            target.setPreviousHighBreakoutScore(
                    overrides.previousHighBreakoutScore()
            );
        }
        if (overrides.previousHighNotBrokenPenalty() != null) {
            target.setPreviousHighNotBrokenPenalty(
                    overrides.previousHighNotBrokenPenalty()
            );
        }
        if (overrides.openingPriceHeldScore() != null) {
            target.setOpeningPriceHeldScore(
                    overrides.openingPriceHeldScore()
            );
        }
        if (overrides.openingPriceLostPenalty() != null) {
            target.setOpeningPriceLostPenalty(
                    overrides.openingPriceLostPenalty()
            );
        }
    }
}
