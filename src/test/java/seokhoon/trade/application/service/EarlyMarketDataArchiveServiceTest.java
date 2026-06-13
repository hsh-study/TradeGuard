package seokhoon.trade.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.EarlyMarketDataCaptureResult;
import seokhoon.trade.application.port.out.AfterHoursMarketDataPort;
import seokhoon.trade.application.port.out.EarlyMarketAfterHoursSnapshotPort;
import seokhoon.trade.application.port.out.EarlyMarketDataCapturePort;
import seokhoon.trade.application.port.out.EarlyMarketIntradayBarSnapshotPort;
import seokhoon.trade.application.port.out.EarlyMarketMarketSnapshotArchivePort;
import seokhoon.trade.application.port.out.EarlyMarketRankingSnapshotPort;
import seokhoon.trade.application.port.out.IntradayBarPort;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.port.out.MarketRankingPort;
import seokhoon.trade.application.port.out.MarketRankingStock;
import seokhoon.trade.application.port.out.MarketSnapshotPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.domain.market.AfterHoursQuote;
import seokhoon.trade.domain.market.EarlyMarketCaptureStatus;
import seokhoon.trade.domain.market.EarlyMarketCaptureType;
import seokhoon.trade.domain.stock.Market;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EarlyMarketDataArchiveServiceTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);
    private static final LocalDate PREVIOUS_DATE = LocalDate.of(2026, 6, 9);
    private static final Instant NOW = Instant.parse("2026-06-10T00:30:00Z");

    private MarketRankingPort rankingPort;
    private AfterHoursMarketDataPort afterHoursPort;
    private MarketCalendarPort calendarPort;
    private EarlyMarketDataCapturePort capturePort;
    private EarlyMarketRankingSnapshotPort rankingArchivePort;
    private EarlyMarketAfterHoursSnapshotPort afterHoursArchivePort;
    private EarlyMarketDataArchiveService service;

    @BeforeEach
    void setUp() {
        rankingPort = mock(MarketRankingPort.class);
        afterHoursPort = mock(AfterHoursMarketDataPort.class);
        calendarPort = mock(MarketCalendarPort.class);
        capturePort = mock(EarlyMarketDataCapturePort.class);
        rankingArchivePort = mock(EarlyMarketRankingSnapshotPort.class);
        afterHoursArchivePort = mock(EarlyMarketAfterHoursSnapshotPort.class);
        when(capturePort.save(any())).thenAnswer(invocation ->
                invocation.getArgument(0)
        );
        when(rankingArchivePort.saveAll(any())).thenAnswer(invocation ->
                invocation.getArgument(0)
        );
        when(afterHoursArchivePort.upsertAfterHours(any())).thenAnswer(
                invocation -> invocation.getArgument(0)
        );
        when(calendarPort.previousTradingDay(TRADE_DATE))
                .thenReturn(PREVIOUS_DATE);
        service = new EarlyMarketDataArchiveService(
                rankingPort,
                afterHoursPort,
                mock(IntradayBarPort.class),
                mock(MarketSnapshotPort.class),
                calendarPort,
                mock(TradingSignalQueryPort.class),
                capturePort,
                rankingArchivePort,
                afterHoursArchivePort,
                mock(EarlyMarketIntradayBarSnapshotPort.class),
                mock(EarlyMarketMarketSnapshotArchivePort.class),
                OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void capturesRankingsAndAfterHoursSuccessfully() {
        stubAllRankings(List.of(stock()));
        when(afterHoursPort.findByStockCode("005930", PREVIOUS_DATE))
                .thenReturn(Optional.of(afterHoursQuote()));

        EarlyMarketDataCaptureResult result = service.capturePreOpen(TRADE_DATE);

        assertThat(result.captures())
                .extracting(capture -> capture.status())
                .containsOnly(EarlyMarketCaptureStatus.SUCCEEDED);
        verify(rankingArchivePort).saveAll(any());
        verify(afterHoursArchivePort).upsertAfterHours(any());
    }

    @Test
    void recordsPartialWhenSomeRankingLookupsFail() {
        when(rankingPort.findTopTradingValueStocks(any(), anyInt()))
                .thenReturn(List.of(stock()));
        when(rankingPort.findTopRisingStocks(any(), anyInt()))
                .thenThrow(new IllegalStateException("unavailable"));
        when(rankingPort.findVolumeSurgeStocks(any(), anyInt()))
                .thenReturn(List.of(stock()));
        when(afterHoursPort.findByStockCode("005930", PREVIOUS_DATE))
                .thenReturn(Optional.of(afterHoursQuote()));

        EarlyMarketDataCaptureResult result = service.capturePreOpen(TRADE_DATE);

        assertThat(result.captures().getFirst().status())
                .isEqualTo(EarlyMarketCaptureStatus.PARTIAL);
    }

    @Test
    void recordsFailedWhenEveryRankingLookupFails() {
        when(rankingPort.findTopTradingValueStocks(any(), anyInt()))
                .thenThrow(new IllegalStateException("unavailable"));
        when(rankingPort.findTopRisingStocks(any(), anyInt()))
                .thenThrow(new IllegalStateException("unavailable"));
        when(rankingPort.findVolumeSurgeStocks(any(), anyInt()))
                .thenThrow(new IllegalStateException("unavailable"));

        EarlyMarketDataCaptureResult result = service.capturePreOpen(TRADE_DATE);

        assertThat(result.captures().getFirst().status())
                .isEqualTo(EarlyMarketCaptureStatus.FAILED);
        assertThat(result.captures().get(1).status())
                .isEqualTo(EarlyMarketCaptureStatus.SKIPPED);
    }

    @Test
    void recordsSkippedWhenRankingQueriesSucceedWithoutItems() {
        stubAllRankings(List.of());

        EarlyMarketDataCaptureResult result = service.capturePreOpen(TRADE_DATE);

        assertThat(result.captures())
                .extracting(capture -> capture.captureType())
                .containsExactly(
                        EarlyMarketCaptureType.PRE_OPEN_RANKING_0830,
                        EarlyMarketCaptureType.AFTER_HOURS_PREVIOUS_DAY
                );
        assertThat(result.captures())
                .extracting(capture -> capture.status())
                .containsOnly(EarlyMarketCaptureStatus.SKIPPED);
    }

    private void stubAllRankings(List<MarketRankingStock> stocks) {
        when(rankingPort.findTopTradingValueStocks(any(), anyInt()))
                .thenReturn(stocks);
        when(rankingPort.findTopRisingStocks(any(), anyInt()))
                .thenReturn(stocks);
        when(rankingPort.findVolumeSurgeStocks(any(), anyInt()))
                .thenReturn(stocks);
    }

    private static MarketRankingStock stock() {
        return new MarketRankingStock(
                "005930", "삼성전자", Market.KOSPI,
                BigDecimal.valueOf(75000), BigDecimal.valueOf(2.1),
                BigDecimal.valueOf(75_000_000), 1000
        );
    }

    private static AfterHoursQuote afterHoursQuote() {
        return new AfterHoursQuote(
                "005930", "삼성전자", PREVIOUS_DATE,
                BigDecimal.valueOf(75100), BigDecimal.valueOf(0.2),
                100, BigDecimal.valueOf(7_510_000), NOW
        );
    }
}
