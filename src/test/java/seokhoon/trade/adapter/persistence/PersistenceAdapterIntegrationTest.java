package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.AnalyzeStockUseCase;
import seokhoon.trade.application.port.in.FindStocksUseCase;
import seokhoon.trade.application.port.in.LoadDailyPricesUseCase;
import seokhoon.trade.application.port.in.LoadIndicatorSnapshotsUseCase;
import seokhoon.trade.application.port.in.RegisterStockUseCase;
import seokhoon.trade.application.port.in.SaveDailyPricesUseCase;
import seokhoon.trade.application.port.in.SaveIndicatorSnapshotUseCase;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PersistenceAdapterIntegrationTest {
    @Autowired
    private RegisterStockUseCase registerStockUseCase;

    @Autowired
    private FindStocksUseCase findStocksUseCase;

    @Autowired
    private SaveDailyPricesUseCase saveDailyPricesUseCase;

    @Autowired
    private LoadDailyPricesUseCase loadDailyPricesUseCase;

    @Autowired
    private SaveIndicatorSnapshotUseCase saveIndicatorSnapshotUseCase;

    @Autowired
    private LoadIndicatorSnapshotsUseCase loadIndicatorSnapshotsUseCase;

    @Autowired
    private IndicatorSnapshotJpaRepository indicatorSnapshotJpaRepository;

    @Autowired
    private AnalyzeStockUseCase analyzeStockUseCase;

    @Autowired
    private TradingSignalJpaRepository tradingSignalJpaRepository;

    @Autowired
    private TradingSignalPort tradingSignalPort;

    @Test
    void persistsStocksWithoutExposingJpaEntities() {
        registerStockUseCase.register("005930", "삼성전자", Market.KOSPI);

        assertThat(findStocksUseCase.findAll())
                .singleElement()
                .satisfies(stock -> {
                    assertThat(stock.stockCode()).isEqualTo("005930");
                    assertThat(stock.stockName()).isEqualTo("삼성전자");
                    assertThat(stock.market()).isEqualTo(Market.KOSPI);
                });
    }

    @Test
    void upsertsAndLoadsDailyPriceByCompositeKey() {
        LocalDate tradeDate = LocalDate.of(2026, 6, 5);
        saveDailyPricesUseCase.saveAll(List.of(price(tradeDate, "70000")));
        saveDailyPricesUseCase.saveAll(List.of(price(tradeDate, "71000")));

        assertThat(loadDailyPricesUseCase.load(
                "005930",
                tradeDate.minusDays(1),
                tradeDate.plusDays(1)
        ))
                .singleElement()
                .extracting(DailyPrice::closePrice)
                .isEqualTo(new BigDecimal("71000"));
    }

    @Test
    void upsertsAndLoadsIndicatorSnapshotByStockAndTradeDate() {
        LocalDate tradeDate = LocalDate.of(2026, 6, 5);
        saveIndicatorSnapshotUseCase.save(snapshot(tradeDate, "70000"));
        saveIndicatorSnapshotUseCase.save(snapshot(tradeDate, "71000"));

        assertThat(loadIndicatorSnapshotsUseCase.load(
                "005930",
                tradeDate.minusDays(1),
                tradeDate.plusDays(1)
        ))
                .singleElement()
                .extracting(IndicatorSnapshot::ma5)
                .isEqualTo(new BigDecimal("71000"));
        assertThat(indicatorSnapshotJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void analyzesStoredPricesIdempotently() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        List<DailyPrice> prices = java.util.stream.IntStream.range(0, 70)
                .mapToObj(index -> price(start.plusDays(index), Integer.toString(70_000 + index * 100)))
                .toList();
        saveDailyPricesUseCase.saveAll(prices);
        LocalDate asOfDate = prices.getLast().tradeDate();

        var first = analyzeStockUseCase.analyze("005930", asOfDate);
        var second = analyzeStockUseCase.analyze("005930", asOfDate);

        assertThat(first.indicatorSnapshot().tradeDate()).isEqualTo(asOfDate);
        assertThat(first.tradingSignal().signalDate()).isEqualTo(asOfDate);
        assertThat(second.tradingSignal().score()).isEqualTo(first.tradingSignal().score());
        assertThat(indicatorSnapshotJpaRepository.count()).isEqualTo(1);
        assertThat(tradingSignalJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void updatesExistingTradingSignalStatus() {
        TradingSignal signal = new TradingSignal(
                "CLOSING_BET",
                "005930",
                LocalDate.of(2026, 6, 5),
                SignalType.BUY_CANDIDATE,
                80,
                List.of("TEST")
        );
        tradingSignalPort.save(signal);
        signal.approveRisk();
        tradingSignalPort.save(signal);

        assertThat(tradingSignalJpaRepository.count()).isEqualTo(1);
        assertThat(tradingSignalJpaRepository.findAll())
                .singleElement()
                .extracting(TradingSignalEntity::status)
                .isEqualTo(TradingSignalStatus.RISK_APPROVED);
    }

    @Test
    void persistsTradingSignalRiskRejectionReasons() {
        TradingSignal signal = new TradingSignal(
                "CLOSING_BET",
                "000660",
                LocalDate.of(2026, 6, 5),
                SignalType.BUY_CANDIDATE,
                60,
                List.of("TEST")
        );
        signal.rejectRisk(List.of("SCORE_BELOW_70", "DUPLICATE_ORDER"));

        tradingSignalPort.save(signal);

        assertThat(tradingSignalJpaRepository.findAll())
                .filteredOn(entity -> entity.status() == TradingSignalStatus.RISK_REJECTED)
                .singleElement()
                .satisfies(entity -> assertThat(entity.riskReasons())
                        .containsExactly("SCORE_BELOW_70", "DUPLICATE_ORDER"));
    }

    @Test
    void restoresStoredTradingSignalForOrderRequest() {
        LocalDate signalDate = LocalDate.of(2026, 6, 5);
        TradingSignal signal = new TradingSignal(
                "CLOSING_BET",
                "035420",
                signalDate,
                SignalType.BUY_CANDIDATE,
                80,
                List.of("TEST")
        );
        signal.rejectRisk(List.of("DUPLICATE_ORDER"));
        tradingSignalPort.save(signal);

        assertThat(tradingSignalPort.find(
                "CLOSING_BET",
                "035420",
                signalDate,
                SignalType.BUY_CANDIDATE
        ))
                .hasValueSatisfying(restored -> {
                    assertThat(restored.score()).isEqualTo(80);
                    assertThat(restored.status()).isEqualTo(TradingSignalStatus.RISK_REJECTED);
                    assertThat(restored.riskReasons()).containsExactly("DUPLICATE_ORDER");
                });
    }

    private static DailyPrice price(LocalDate tradeDate, String closePrice) {
        BigDecimal close = new BigDecimal(closePrice);
        return new DailyPrice(
                "005930",
                tradeDate,
                close,
                close.add(BigDecimal.valueOf(1_000)),
                close.subtract(BigDecimal.valueOf(1_000)),
                close,
                1_000_000L,
                close.multiply(BigDecimal.valueOf(1_000_000L))
        );
    }

    private static IndicatorSnapshot snapshot(LocalDate tradeDate, String ma5) {
        return new IndicatorSnapshot(
                "005930",
                tradeDate,
                new BigDecimal(ma5),
                new BigDecimal("69000"),
                new BigDecimal("65000"),
                new BigDecimal("55"),
                new BigDecimal("100"),
                new BigDecimal("80"),
                new BigDecimal("20"),
                new BigDecimal("75000"),
                new BigDecimal("69000"),
                new BigDecimal("63000")
        );
    }
}
