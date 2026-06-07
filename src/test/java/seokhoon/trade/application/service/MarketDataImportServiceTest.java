package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.application.port.out.MarketDataPort;
import seokhoon.trade.domain.market.DailyPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketDataImportServiceTest {
    @Test
    void fetchesAndPersistsDailyPrices() {
        DailyPrice fetched = price(LocalDate.of(2026, 6, 5));
        RecordingMarketDataPort marketDataPort = new RecordingMarketDataPort(List.of(fetched));
        RecordingDailyPricePort dailyPricePort = new RecordingDailyPricePort();
        MarketDataImportService service = new MarketDataImportService(marketDataPort, dailyPricePort);

        List<DailyPrice> result = service.importPrices(
                "005930",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 7)
        );

        assertThat(result).containsExactly(fetched);
        assertThat(dailyPricePort.saved).containsExactly(fetched);
    }

    @Test
    void doesNotCallPersistenceWhenBrokerReturnsNoPrices() {
        RecordingDailyPricePort dailyPricePort = new RecordingDailyPricePort();
        MarketDataImportService service = new MarketDataImportService(
                new RecordingMarketDataPort(List.of()),
                dailyPricePort
        );

        List<DailyPrice> result = service.importPrices(
                "005930",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 7)
        );

        assertThat(result).isEmpty();
        assertThat(dailyPricePort.saveCalls).isZero();
    }

    @Test
    void validatesDateRangeBeforeCallingExternalApi() {
        RecordingMarketDataPort marketDataPort = new RecordingMarketDataPort(List.of());
        MarketDataImportService service = new MarketDataImportService(
                marketDataPort,
                new RecordingDailyPricePort()
        );

        assertThatThrownBy(() -> service.importPrices(
                "005930",
                LocalDate.of(2026, 6, 7),
                LocalDate.of(2026, 6, 1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from must not be after to");
        assertThat(marketDataPort.calls).isZero();
    }

    private static DailyPrice price(LocalDate tradeDate) {
        return new DailyPrice(
                "005930",
                tradeDate,
                new BigDecimal("70000"),
                new BigDecimal("72000"),
                new BigDecimal("69000"),
                new BigDecimal("71000"),
                1_000_000L,
                new BigDecimal("71000000000")
        );
    }

    private static class RecordingMarketDataPort implements MarketDataPort {
        private final List<DailyPrice> result;
        private int calls;

        private RecordingMarketDataPort(List<DailyPrice> result) {
            this.result = result;
        }

        @Override
        public List<DailyPrice> fetchDailyPrices(String stockCode, LocalDate from, LocalDate to) {
            calls++;
            return result;
        }
    }

    private static class RecordingDailyPricePort implements DailyPricePort {
        private List<DailyPrice> saved = List.of();
        private int saveCalls;

        @Override
        public List<DailyPrice> saveAll(List<DailyPrice> dailyPrices) {
            saveCalls++;
            saved = List.copyOf(dailyPrices);
            return saved;
        }

        @Override
        public List<DailyPrice> findByStockCodeAndTradeDateBetween(
                String stockCode,
                LocalDate from,
                LocalDate to
        ) {
            return List.of();
        }
    }
}
