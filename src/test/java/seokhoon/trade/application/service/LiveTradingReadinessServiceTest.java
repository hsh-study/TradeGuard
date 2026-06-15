package seokhoon.trade.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.LiveTradingProperties;
import seokhoon.trade.domain.kis.*;
import seokhoon.trade.domain.order.*;

import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LiveTradingReadinessServiceTest {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private static final Instant OPEN_TIME=ZonedDateTime.of(
            LocalDate.of(2026,6,15),LocalTime.of(10,0),SEOUL).toInstant();
    private LiveTradingProperties live;
    private KisConfigurationPort kis;
    private KisAccessTokenProvider tokens;
    private LiveTradingRuntimeStatePort runtime;
    private MarketCalendarPort calendar;
    private MarketCalendarDayPort calendarDays;
    private OperationalMetricsPort metrics;

    @BeforeEach
    void setUp() {
        live=new LiveTradingProperties();
        live.setLiveTradingEnabled(true);
        live.setKisTradingEnabled(true);
        live.setAccountNumber("secret-account");
        live.setAccountProductCode("01");
        live.setKisEnvironment("REAL");
        kis=mock(KisConfigurationPort.class);
        when(kis.credentialsConfigured()).thenReturn(true);
        when(kis.readOnlyEnvironment()).thenReturn(KisEnvironment.DEMO);
        when(kis.tokenRefreshBeforeSeconds()).thenReturn(600);
        tokens=mock(KisAccessTokenProvider.class);
        runtime=mock(LiveTradingRuntimeStatePort.class);
        calendar=mock(MarketCalendarPort.class);
        calendarDays=mock(MarketCalendarDayPort.class);
        metrics=mock(OperationalMetricsPort.class);
        when(runtime.get()).thenReturn(new LiveTradingRuntimeState(
                false,null,OPEN_TIME));
        when(calendar.isTradingDay(LocalDate.of(2026,6,15)))
                .thenReturn(true);
        when(calendarDays.existsByYear(2026)).thenReturn(true);
        when(tokens.findTokenMetadata(KisEnvironment.REAL))
                .thenReturn(Optional.of(new KisAccessToken(
                        KisEnvironment.REAL,"raw-secret-token","Bearer",
                        OPEN_TIME.plusSeconds(3600),OPEN_TIME,"safe")));
    }

    @Test
    void reportsReadyWhenAllBlockingChecksPass() {
        LiveTradingReadinessReport report=service(OPEN_TIME)
                .checkReadiness();

        assertThat(report.ready()).isTrue();
        assertThat(report.blockingReasons()).isEmpty();
        assertThat(report.marketOpenNow()).isTrue();
        verify(metrics).recordLiveTradingReadiness("ready");
    }

    @Test
    void blocksWhenFeatureFlagsAreOff() {
        live.setLiveTradingEnabled(false);
        live.setKisTradingEnabled(false);

        LiveTradingReadinessReport report=service(OPEN_TIME)
                .checkReadiness();

        assertThat(report.ready()).isFalse();
        assertThat(report.blockingReasons()).contains(
                "LIVE_TRADING_DISABLED","KIS_TRADING_DISABLED");
    }

    @Test
    void blocksWhenTokenIsMissing() {
        when(tokens.findTokenMetadata(KisEnvironment.REAL))
                .thenReturn(Optional.empty());

        LiveTradingReadinessReport report=service(OPEN_TIME)
                .checkReadiness();

        assertThat(report.tokenStatus().tokenPresent()).isFalse();
        assertThat(report.blockingReasons()).contains("KIS_TOKEN_MISSING");
    }

    @Test
    void blocksWhenKillSwitchIsEnabled() {
        when(runtime.get()).thenReturn(new LiveTradingRuntimeState(
                true,"operator",OPEN_TIME));

        assertThat(service(OPEN_TIME).checkReadiness().blockingReasons())
                .contains("KILL_SWITCH_ENABLED");
    }

    @Test
    void blocksWhenAccountOrCalendarIsMissing() {
        live.setAccountNumber("");
        when(calendarDays.existsByYear(2026)).thenReturn(false);

        LiveTradingReadinessReport report=service(OPEN_TIME)
                .checkReadiness();

        assertThat(report.accountConfigured()).isFalse();
        assertThat(report.blockingReasons()).contains(
                "KIS_ACCOUNT_NOT_CONFIGURED",
                "MARKET_CALENDAR_DB_MISSING");
        assertThat(report.marketCalendarStatus().source())
                .isEqualTo("RUNTIME_FALLBACK");
    }

    @Test
    void marketClosedIsWarningRatherThanBlocking() {
        Instant closed=ZonedDateTime.of(LocalDate.of(2026,6,15),
                LocalTime.of(18,0),SEOUL).toInstant();
        when(tokens.findTokenMetadata(KisEnvironment.REAL))
                .thenReturn(Optional.of(new KisAccessToken(
                        KisEnvironment.REAL,"token","Bearer",
                        closed.plusSeconds(3600),closed,"safe")));

        LiveTradingReadinessReport report=service(closed)
                .checkReadiness();

        assertThat(report.ready()).isTrue();
        assertThat(report.marketOpenNow()).isFalse();
        assertThat(report.warnings()).contains("MARKET_CLOSED_NOW");
    }

    private LiveTradingReadinessService service(Instant instant) {
        return new LiveTradingReadinessService(live,kis,tokens,runtime,
                calendar,calendarDays,metrics,
                Clock.fixed(instant,SEOUL));
    }
}
