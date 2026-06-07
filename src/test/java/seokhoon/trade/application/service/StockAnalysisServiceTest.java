package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.TestPrices;
import seokhoon.trade.application.port.in.AnalysisResult;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.application.port.out.IndicatorSnapshotPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.indicator.TechnicalIndicatorCalculator;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.strategy.ClosingBetStrategy;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockAnalysisServiceTest {
    @Test
    void calculatesAndPersistsIndicatorAndSignal() {
        List<DailyPrice> prices = TestPrices.risingPrices(70);
        RecordingDailyPricePort dailyPricePort = new RecordingDailyPricePort(prices);
        RecordingIndicatorSnapshotPort indicatorPort = new RecordingIndicatorSnapshotPort();
        RecordingTradingSignalPort signalPort = new RecordingTradingSignalPort();
        StockAnalysisService service = service(dailyPricePort, indicatorPort, signalPort);
        LocalDate asOfDate = prices.getLast().tradeDate();

        AnalysisResult result = service.analyze("005930", asOfDate);

        assertThat(result.indicatorSnapshot()).isSameAs(indicatorPort.saved);
        assertThat(result.tradingSignal()).isSameAs(signalPort.saved);
        assertThat(result.indicatorSnapshot().tradeDate()).isEqualTo(asOfDate);
        assertThat(result.tradingSignal().signalDate()).isEqualTo(asOfDate);
        assertThat(dailyPricePort.requestedFrom).isEqualTo(asOfDate.minusYears(1));
        assertThat(dailyPricePort.requestedTo).isEqualTo(asOfDate);
    }

    @Test
    void rejectsAnalysisWhenFewerThanSixtyPricesExist() {
        StockAnalysisService service = service(
                new RecordingDailyPricePort(TestPrices.risingPrices(59)),
                new RecordingIndicatorSnapshotPort(),
                new RecordingTradingSignalPort()
        );

        assertThatThrownBy(() -> service.analyze("005930", LocalDate.of(2026, 6, 7)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("At least 60 daily prices are required for analysis");
    }

    @Test
    void validatesStockCodeBeforeLoadingPrices() {
        RecordingDailyPricePort dailyPricePort = new RecordingDailyPricePort(List.of());
        StockAnalysisService service = service(
                dailyPricePort,
                new RecordingIndicatorSnapshotPort(),
                new RecordingTradingSignalPort()
        );

        assertThatThrownBy(() -> service.analyze(" ", LocalDate.of(2026, 6, 7)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stockCode must not be blank");
        assertThat(dailyPricePort.calls).isZero();
    }

    private static StockAnalysisService service(
            DailyPricePort dailyPricePort,
            IndicatorSnapshotPort indicatorPort,
            TradingSignalPort signalPort
    ) {
        return new StockAnalysisService(
                dailyPricePort,
                indicatorPort,
                signalPort,
                new TechnicalIndicatorCalculator(),
                new ClosingBetStrategy()
        );
    }

    private static class RecordingDailyPricePort implements DailyPricePort {
        private final List<DailyPrice> prices;
        private int calls;
        private LocalDate requestedFrom;
        private LocalDate requestedTo;

        private RecordingDailyPricePort(List<DailyPrice> prices) {
            this.prices = prices;
        }

        @Override
        public List<DailyPrice> saveAll(List<DailyPrice> dailyPrices) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DailyPrice> findByStockCodeAndTradeDateBetween(
                String stockCode,
                LocalDate from,
                LocalDate to
        ) {
            calls++;
            requestedFrom = from;
            requestedTo = to;
            return prices;
        }
    }

    private static class RecordingIndicatorSnapshotPort implements IndicatorSnapshotPort {
        private IndicatorSnapshot saved;

        @Override
        public IndicatorSnapshot save(IndicatorSnapshot snapshot) {
            saved = snapshot;
            return snapshot;
        }

        @Override
        public List<IndicatorSnapshot> findByStockCodeAndTradeDateBetween(
                String stockCode,
                LocalDate from,
                LocalDate to
        ) {
            return List.of();
        }
    }

    private static class RecordingTradingSignalPort implements TradingSignalPort {
        private TradingSignal saved;

        @Override
        public TradingSignal save(TradingSignal tradingSignal) {
            saved = tradingSignal;
            return tradingSignal;
        }

        @Override
        public Optional<TradingSignal> find(
                String strategyName,
                String stockCode,
                LocalDate signalDate,
                seokhoon.trade.domain.strategy.SignalType signalType
        ) {
            return Optional.empty();
        }
    }
}
