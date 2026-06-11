package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EarlyMarketPriceActionFeatures(
        String stockCode,
        LocalDate tradeDate,
        LocalDate previousTradingDay,
        BigDecimal previousHigh,
        BigDecimal openingPrice,
        BigDecimal lastPrice,
        Boolean brokePreviousHigh,
        Boolean heldOpeningPrice,
        Boolean pullbackRecovered,
        boolean dataSufficient,
        List<String> reasons
) {
}
