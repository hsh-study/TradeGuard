package seokhoon.trade.domain.order;

import seokhoon.trade.domain.kis.KisEnvironment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record LiveTradingReadinessReport(
        boolean liveTradingEnabled,
        boolean kisTradingEnabled,
        KisEnvironment tradingEnvironment,
        KisEnvironment readOnlyEnvironment,
        boolean accountConfigured,
        TokenStatus tokenStatus,
        boolean killSwitchEnabled,
        MarketCalendarStatus marketCalendarStatus,
        boolean marketOpenNow,
        String orderTypePolicy,
        BigDecimal maxAllowedOrderAmount,
        boolean taxAndFeeConfigured,
        AutoCancelPolicy autoCancelPolicy,
        List<String> warnings,
        List<String> blockingReasons,
        boolean ready
) {
    public record TokenStatus(
            boolean tokenPresent,
            Instant expiresAt,
            long secondsToExpire,
            String status
    ) {}

    public record MarketCalendarStatus(
            boolean currentYearDataPresent,
            boolean tradingDayToday,
            String source
    ) {}

    public record AutoCancelPolicy(
            boolean enabled,
            int buyOrderExpireMinutes,
            int sellOrderExpireMinutes,
            int cancelBeforeMarketCloseMinutes
    ) {}
}
