package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.FindStocksUseCase;
import seokhoon.trade.application.port.in.LoadDailyPricesUseCase;
import seokhoon.trade.application.port.in.LoadIndicatorSnapshotsUseCase;
import seokhoon.trade.application.port.in.RegisterStockUseCase;
import seokhoon.trade.application.port.in.SaveDailyPricesUseCase;
import seokhoon.trade.application.port.in.SaveIndicatorSnapshotUseCase;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.stock.Market;

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
