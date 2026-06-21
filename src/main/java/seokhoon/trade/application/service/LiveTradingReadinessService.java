package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.LiveTradingReadinessUseCase;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.LiveTradingProperties;
import seokhoon.trade.domain.kis.*;
import seokhoon.trade.domain.order.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class LiveTradingReadinessService
        implements LiveTradingReadinessUseCase {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private static final LocalTime MARKET_OPEN=LocalTime.of(9,0);
    private static final LocalTime MARKET_CLOSE=LocalTime.of(15,30);

    private final LiveTradingProperties live;
    private final KisConfigurationPort kis;
    private final KisAccessTokenProvider tokens;
    private final LiveTradingRuntimeStatePort runtime;
    private final MarketCalendarPort calendar;
    private final MarketCalendarDayPort calendarDays;
    private final OperationalMetricsPort metrics;
    private final TradingAccountManagementUseCase accounts;
    private final Clock clock;

    @Autowired
    public LiveTradingReadinessService(
            LiveTradingProperties live,
            KisConfigurationPort kis,
            KisAccessTokenProvider tokens,
            LiveTradingRuntimeStatePort runtime,
            MarketCalendarPort calendar,
            MarketCalendarDayPort calendarDays,
            OperationalMetricsPort metrics,
            TradingAccountManagementUseCase accounts
    ) {
        this(live,kis,tokens,runtime,calendar,calendarDays,metrics,accounts,
                Clock.system(SEOUL));
    }

    LiveTradingReadinessService(
            LiveTradingProperties live,
            KisConfigurationPort kis,
            KisAccessTokenProvider tokens,
            LiveTradingRuntimeStatePort runtime,
            MarketCalendarPort calendar,
            MarketCalendarDayPort calendarDays,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this(live,kis,tokens,runtime,calendar,calendarDays,metrics,null,clock);
    }

    LiveTradingReadinessService(
            LiveTradingProperties live,
            KisConfigurationPort kis,
            KisAccessTokenProvider tokens,
            LiveTradingRuntimeStatePort runtime,
            MarketCalendarPort calendar,
            MarketCalendarDayPort calendarDays,
            OperationalMetricsPort metrics,
            TradingAccountManagementUseCase accounts,
            Clock clock
    ) {
        this.live=live;
        this.kis=kis;
        this.tokens=tokens;
        this.runtime=runtime;
        this.calendar=calendar;
        this.calendarDays=calendarDays;
        this.metrics=metrics;
        this.accounts=accounts;
        this.clock=clock;
    }

    @Override
    public LiveTradingReadinessReport checkReadiness() {
        List<String> blocking=new ArrayList<>();
        List<String> warnings=new ArrayList<>();
        if (!live.isLiveTradingEnabled()) {
            blocking.add("LIVE_TRADING_DISABLED");
        }
        if (!live.isKisTradingEnabled()) {
            blocking.add("KIS_TRADING_DISABLED");
        }

        KisEnvironment tradingEnvironment=accounts == null ? environment(blocking)
                : accounts.primaryCredentials().map(TradingAccountManagementUseCase.AccountCredentials::environment)
                        .orElseGet(() -> environment(blocking));
        boolean accountConfigured=accounts == null
                ? hasText(live.getAccountNumber()) && hasText(live.getAccountProductCode())
                : accounts.primaryCredentials().isPresent();
        if (!accountConfigured) blocking.add("KIS_ACCOUNT_NOT_CONFIGURED");
        if (accounts != null && !accounts.encryptionConfigured()) {
            blocking.add("KIS_ACCOUNT_ENCRYPTION_KEY_NOT_CONFIGURED");
        }
        if (!kis.credentialsConfigured()) {
            blocking.add("KIS_CREDENTIALS_NOT_CONFIGURED");
        }
        if (kis.tokenCacheMode() == KisTokenCacheMode.DB
                && !kis.tokenEncryptionConfigured()) {
            blocking.add("KIS_TOKEN_ENCRYPTION_KEY_NOT_CONFIGURED");
        }

        LiveTradingRuntimeState state=runtime.get();
        if (state.killSwitchEnabled()) {
            blocking.add("KILL_SWITCH_ENABLED");
        }

        ZonedDateTime now=ZonedDateTime.now(clock).withZoneSameInstant(SEOUL);
        LocalDate today=now.toLocalDate();
        boolean currentYearData=calendarDays.existsByYear(today.getYear());
        boolean tradingDay=calendar.isTradingDay(today);
        if (!currentYearData) {
            blocking.add("MARKET_CALENDAR_DB_MISSING");
        }
        boolean marketOpen=tradingDay
                && !now.toLocalTime().isBefore(MARKET_OPEN)
                && !now.toLocalTime().isAfter(MARKET_CLOSE);
        if (!marketOpen) warnings.add("MARKET_CLOSED_NOW");

        LiveTradingReadinessReport.TokenStatus tokenStatus =
                tokenStatus(tradingEnvironment,blocking);
        boolean policyConfigured=policyConfigured(blocking);
        if (!live.isLiveOrderAutoCancelEnabled()) {
            warnings.add("LIVE_ORDER_AUTO_CANCEL_DISABLED");
        }

        boolean ready=blocking.isEmpty();
        metrics.recordLiveTradingReadiness(ready ? "ready" : "blocked");
        return new LiveTradingReadinessReport(
                live.isLiveTradingEnabled(),
                live.isKisTradingEnabled(),
                tradingEnvironment,
                kis.readOnlyEnvironment(),
                accountConfigured,
                tokenStatus,
                state.killSwitchEnabled(),
                new LiveTradingReadinessReport.MarketCalendarStatus(
                        currentYearData,tradingDay,
                        currentYearData ? "DB" : "RUNTIME_FALLBACK"),
                marketOpen,
                live.getAllowedOrderType() == null
                        ? "UNCONFIGURED" : live.getAllowedOrderType().name(),
                live.getMaxAllowedOrderAmount(),
                policyConfigured,
                new LiveTradingReadinessReport.AutoCancelPolicy(
                        live.isLiveOrderAutoCancelEnabled(),
                        live.getBuyOrderExpireMinutes(),
                        live.getSellOrderExpireMinutes(),
                        live.getCancelBeforeMarketCloseMinutes()),
                List.copyOf(warnings),
                List.copyOf(blocking),
                ready);
    }

    private KisEnvironment environment(List<String> blocking) {
        try {
            return live.environment();
        } catch (RuntimeException exception) {
            blocking.add("KIS_TRADING_ENVIRONMENT_INVALID");
            return KisEnvironment.REAL;
        }
    }

    private LiveTradingReadinessReport.TokenStatus tokenStatus(
            KisEnvironment environment,List<String> blocking) {
        Instant now=clock.instant();
        Optional<KisAccessToken> optional=tokens.findTokenMetadata(environment);
        if (optional.isEmpty()) {
            blocking.add("KIS_TOKEN_MISSING");
            return new LiveTradingReadinessReport.TokenStatus(
                    false,null,0,"MISSING");
        }
        KisAccessToken token=optional.get();
        long seconds=Duration.between(now,token.expiresAt()).getSeconds();
        String status;
        if (seconds <= 0) {
            status="EXPIRED";
            blocking.add("KIS_TOKEN_EXPIRED");
        } else if (seconds <= kis.tokenRefreshBeforeSeconds()) {
            status="EXPIRING";
            blocking.add("KIS_TOKEN_EXPIRING");
        } else {
            status="VALID";
        }
        return new LiveTradingReadinessReport.TokenStatus(
                true,token.expiresAt(),seconds,status);
    }

    private boolean policyConfigured(List<String> blocking) {
        boolean valid=nonNegative(live.getBuyCommissionRate())
                && nonNegative(live.getSellCommissionRate())
                && nonNegative(live.getSellTaxRate());
        if (!valid) blocking.add("TAX_OR_FEE_POLICY_INVALID");
        if (live.getAllowedOrderType() != OrderType.LIMIT) {
            blocking.add("ORDER_TYPE_POLICY_NOT_LIMIT");
        }
        if (live.getMaxAllowedOrderAmount() == null
                || live.getMaxAllowedOrderAmount().signum() <= 0) {
            blocking.add("MAX_ORDER_AMOUNT_INVALID");
        }
        return valid;
    }

    private static boolean nonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
