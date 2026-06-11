package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.EarlyMarketPriceActionFeatures;

import java.time.LocalDate;
import java.time.LocalTime;

public interface LoadEarlyMarketPriceActionFeaturesUseCase {
    EarlyMarketPriceActionFeatures load(
            String stockCode,
            LocalDate tradeDate,
            LocalTime to
    );
}
