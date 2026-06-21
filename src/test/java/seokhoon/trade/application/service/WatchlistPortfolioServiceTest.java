package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.application.port.out.LivePositionPort;
import seokhoon.trade.application.port.out.KisAccountBalancePort;
import seokhoon.trade.application.port.out.StockPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.application.port.out.ValuationSnapshotPort;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.position.LivePosition;
import seokhoon.trade.domain.position.LivePositionStatus;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignalStatus;
import seokhoon.trade.domain.research.ValuationSnapshot;
import seokhoon.trade.domain.research.ValuationSnapshotSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WatchlistPortfolioServiceTest {
    @Test
    void includesLatestCloseAndVolumeInWatchlist() {
        StockPort stocks = mock(StockPort.class);
        DailyPricePort prices = mock(DailyPricePort.class);
        LivePositionPort positions = mock(LivePositionPort.class);
        TradingSignalQueryPort signals = mock(TradingSignalQueryPort.class);
        when(stocks.findAll()).thenReturn(List.of(new Stock("005930", "삼성전자", Market.KOSPI, true)));
        when(prices.findLatestByStockCode("005930")).thenReturn(Optional.of(price("354000", 43_284_898L)));

        var rows = new WatchlistPortfolioService(stocks, prices, positions, signals).watchlist();

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.closePrice()).isEqualByComparingTo("354000");
            assertThat(row.volume()).isEqualTo(43_284_898L);
            assertThat(row.latestTradeDate()).isEqualTo(LocalDate.of(2026, 6, 19));
        });
    }

    @Test
    void separatesDemoAndRealHoldingsAndCalculatesValuation() {
        StockPort stocks = mock(StockPort.class);
        DailyPricePort prices = mock(DailyPricePort.class);
        LivePositionPort positions = mock(LivePositionPort.class);
        TradingSignalQueryPort signals = mock(TradingSignalQueryPort.class);
        when(stocks.findAll()).thenReturn(List.of(new Stock("005930", "삼성전자", Market.KOSPI, true)));
        when(prices.findLatestByStockCode("005930")).thenReturn(Optional.of(price("120", 1000L)));
        when(positions.findOpenPositions()).thenReturn(List.of(
                position(1L, KisEnvironment.DEMO), position(2L, KisEnvironment.REAL)));

        var rows = new WatchlistPortfolioService(stocks, prices, positions, signals).holdings();

        assertThat(rows).extracting(row -> row.environmentLabel())
                .containsExactly("모의투자", "실전투자");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.marketValue()).isEqualByComparingTo("1200");
            assertThat(row.unrealizedProfitLoss()).isEqualByComparingTo("200");
            assertThat(row.unrealizedReturnRate()).isEqualByComparingTo("20.0000");
        });
    }

    @Test
    void addsAllOverlappingRecommendationAndUserTags() {
        StockPort stocks = mock(StockPort.class);
        DailyPricePort prices = mock(DailyPricePort.class);
        LivePositionPort positions = mock(LivePositionPort.class);
        TradingSignalQueryPort signals = mock(TradingSignalQueryPort.class);
        when(stocks.findAll()).thenReturn(List.of(
                new Stock("005930", "삼성전자", Market.KOSPI, true)));
        when(prices.findLatestByStockCode("005930"))
                .thenReturn(Optional.of(price("354000", 43_284_898L)));
        when(positions.findOpenPositions()).thenReturn(List.of(position(1L, KisEnvironment.DEMO)));
        when(signals.find(any())).thenAnswer(invocation -> {
            var criteria = invocation.getArgument(0,
                    seokhoon.trade.application.port.in.TradingSignalSearchCriteria.class);
            if (criteria.signalDate().equals(LocalDate.of(2026, 6, 19))) {
                return List.of(signal(criteria.strategyName(), criteria.signalType()));
            }
            return List.of();
        });
        Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"),
                ZoneId.of("Asia/Seoul"));

        var service = new WatchlistPortfolioService(stocks, prices, positions, signals, clock);

        assertThat(service.watchlist()).singleElement()
                .extracting(row -> row.tags())
                .isEqualTo(List.of("장초반", "종베", "사용자"));
        assertThat(service.holdings()).singleElement()
                .extracting(row -> row.tags())
                .isEqualTo(List.of("장초반", "종베", "사용자"));
    }

    @Test
    void exposesSelectedKisAccountHoldingsWithoutPersistedPosition() {
        StockPort stocks = mock(StockPort.class);
        DailyPricePort prices = mock(DailyPricePort.class);
        LivePositionPort positions = mock(LivePositionPort.class);
        TradingSignalQueryPort signals = mock(TradingSignalQueryPort.class);
        KisAccountBalancePort balances = mock(KisAccountBalancePort.class);
        when(balances.holdings(2L)).thenReturn(List.of(new KisAccountBalancePort.AccountHolding(
                KisEnvironment.REAL, "005930", "삼성전자", 3,
                new BigDecimal("70000"), new BigDecimal("210000"),
                new BigDecimal("72000"), new BigDecimal("216000"),
                new BigDecimal("6000"), new BigDecimal("2.85"))));
        var service = new WatchlistPortfolioService(stocks, prices, positions,
                signals, balances, Clock.systemUTC());

        assertThat(service.holdings(2L)).singleElement().satisfies(row -> {
            assertThat(row.positionId()).isNull();
            assertThat(row.environmentLabel()).isEqualTo("실전투자");
            assertThat(row.source()).isEqualTo("KIS_ACCOUNT");
            assertThat(row.quantity()).isEqualTo(3);
        });
    }

    @Test
    void exposesLatestPerAndPbrForWatchlistAndAccountHoldings() {
        StockPort stocks = mock(StockPort.class);
        DailyPricePort prices = mock(DailyPricePort.class);
        LivePositionPort positions = mock(LivePositionPort.class);
        TradingSignalQueryPort signals = mock(TradingSignalQueryPort.class);
        KisAccountBalancePort balances = mock(KisAccountBalancePort.class);
        ValuationSnapshotPort valuations = mock(ValuationSnapshotPort.class);
        when(stocks.findAll()).thenReturn(List.of(new Stock("005930", "삼성전자", Market.KOSPI, true)));
        when(balances.holdings(1L)).thenReturn(List.of(new KisAccountBalancePort.AccountHolding(
                KisEnvironment.DEMO, "005930", "삼성전자", 1, BigDecimal.TEN,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO)));
        Instant now = Instant.parse("2026-06-21T00:00:00Z");
        when(valuations.findLatestByStockCode(eq("005930"), any())).thenReturn(Optional.of(
                new ValuationSnapshot(1L, "005930", LocalDate.of(2026, 6, 20),
                        new BigDecimal("1000000"), new BigDecimal("12.34"),
                        new BigDecimal("1.56"), null, null, null, null,
                        ValuationSnapshotSource.AUTO, now, now)));
        var service = new WatchlistPortfolioService(stocks, prices, positions, signals,
                balances, valuations, Clock.fixed(now, ZoneId.of("Asia/Seoul")));

        assertThat(service.watchlist()).singleElement().satisfies(row -> {
            assertThat(row.per()).isEqualByComparingTo("12.34");
            assertThat(row.pbr()).isEqualByComparingTo("1.56");
        });
        assertThat(service.holdings(1L)).singleElement().satisfies(row -> {
            assertThat(row.per()).isEqualByComparingTo("12.34");
            assertThat(row.pbr()).isEqualByComparingTo("1.56");
        });
    }

    private static TradingSignalRecord signal(String strategyName, SignalType signalType) {
        return new TradingSignalRecord(1L, strategyName, "005930",
                LocalDate.of(2026, 6, 19), signalType, 90, List.of(), List.of(),
                TradingSignalStatus.CREATED);
    }

    private static DailyPrice price(String close, long volume) {
        BigDecimal value = new BigDecimal(close);
        return new DailyPrice("005930", LocalDate.of(2026, 6, 19), value,
                value, value, value, volume, value.multiply(BigDecimal.valueOf(volume)));
    }

    private static LivePosition position(long id, KisEnvironment environment) {
        return new LivePosition(id, "005930", environment, 10,
                new BigDecimal("100"), new BigDecimal("1000"), BigDecimal.ZERO,
                LivePositionStatus.OPEN, Instant.parse("2026-06-01T00:00:00Z"), null);
    }
}
