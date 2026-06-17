package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ResearchUseCases.SaveMarketIndexCommand;
import seokhoon.trade.application.port.out.MarketIndexImportHistoryPort;
import seokhoon.trade.application.port.out.MarketIndexPort;
import seokhoon.trade.application.port.out.MarketIndexProviderPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.config.MarketIndexProviderProperties;
import seokhoon.trade.domain.market.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MarketIndexServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 12);

    private final InMemoryMarketIndexPort indices = new InMemoryMarketIndexPort();
    private final InMemoryMarketIndexHistoryPort histories = new InMemoryMarketIndexHistoryPort();
    private final MarketIndexProviderPort provider = mock(MarketIndexProviderPort.class);
    private final MarketIndexProviderProperties properties = new MarketIndexProviderProperties();
    private final MarketIndexService service = new MarketIndexService(indices, provider, histories,
            properties, OperationalMetricsPort.noop(), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void savesAndFindsManualMarketIndex() {
        MarketIndex saved = service.save(new SaveMarketIndexCommand("KOSPI", "KOSPI", TRADE_DATE,
                bd("2800"), bd("1.2500"), bd("9000000000000")));

        assertThat(saved.indexCode()).isEqualTo("KOSPI");
        assertThat(service.findByTradeDate(TRADE_DATE))
                .extracting(MarketIndex::indexName)
                .containsExactly("KOSPI");
    }

    @Test
    void importsMarketIndexCsv() {
        MarketIndexImportHistory history = service.importCsv("""
                indexCode,indexName,tradeDate,closePrice,changeRate,tradingValue
                KOSPI,KOSPI,2026-06-12,2800,1.25,9000000000000
                KOSDAQ,KOSDAQ,2026-06-12,900,-0.5,3000000000000
                """);

        assertThat(history.status()).isEqualTo(MarketIndexImportStatus.SUCCESS);
        assertThat(history.provider()).isEqualTo(MarketIndexImportProvider.CSV);
        assertThat(history.importedCount()).isEqualTo(2);
        assertThat(service.findByTradeDate(TRADE_DATE)).hasSize(2);
    }

    @Test
    void skipsProviderImportWhenDisabled() {
        properties.setEnabled(false);

        MarketIndexImportHistory history = service.importProvider(TRADE_DATE);

        assertThat(history.status()).isEqualTo(MarketIndexImportStatus.SKIPPED);
        assertThat(history.failureReason()).contains("DISABLED");
        verifyNoInteractions(provider);
    }

    @Test
    void importsEnabledProviderMajorIndices() {
        properties.setEnabled(true);
        when(provider.fetchMajorIndices(TRADE_DATE)).thenReturn(List.of(index("KOSPI"), index("KOSDAQ")));

        MarketIndexImportHistory history = service.importProvider(TRADE_DATE);

        assertThat(history.status()).isEqualTo(MarketIndexImportStatus.SUCCESS);
        assertThat(history.importedCount()).isEqualTo(2);
        assertThat(indices.values).hasSize(2);
    }

    @Test
    void hasNoBrokerOrOrderDependencySoImportCannotExecuteOrders() {
        assertThat(List.of(MarketIndexService.class.getDeclaredConstructors())
                .stream()
                .flatMap(constructor -> List.of(constructor.getParameterTypes()).stream())
                .map(Class::getName))
                .noneMatch(name -> name.contains("Broker") || name.contains("Order"));
    }

    private static MarketIndex index(String code) {
        return new MarketIndex(null, code, code, TRADE_DATE, bd("100"),
                bd("1.0000"), bd("1000"), NOW, NOW);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static class InMemoryMarketIndexPort implements MarketIndexPort {
        private final List<MarketIndex> values = new ArrayList<>();

        @Override
        public MarketIndex save(MarketIndex index) {
            values.removeIf(value -> value.indexCode().equals(index.indexCode())
                    && value.tradeDate().equals(index.tradeDate()));
            MarketIndex saved = new MarketIndex(index.id() == null ? 1L : index.id(),
                    index.indexCode(), index.indexName(), index.tradeDate(), index.closePrice(),
                    index.changeRate(), index.tradingValue(), index.createdAt(), index.updatedAt());
            values.add(saved);
            return saved;
        }

        @Override
        public List<MarketIndex> findByTradeDate(LocalDate tradeDate) {
            return values.stream().filter(value -> value.tradeDate().equals(tradeDate)).toList();
        }
    }

    private static class InMemoryMarketIndexHistoryPort implements MarketIndexImportHistoryPort {
        private final List<MarketIndexImportHistory> values = new ArrayList<>();

        @Override
        public MarketIndexImportHistory save(MarketIndexImportHistory history) {
            MarketIndexImportHistory saved = new MarketIndexImportHistory(1L, history.provider(),
                    history.tradeDate(), history.status(), history.importedCount(),
                    history.failureReason(), history.requestedAt(), history.completedAt());
            values.add(saved);
            return saved;
        }

        @Override
        public List<MarketIndexImportHistory> findRecentMarketIndexImports(int limit) {
            return values.stream().limit(limit).toList();
        }
    }
}
