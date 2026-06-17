package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ResearchUseCases;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.ResearchProperties;
import seokhoon.trade.domain.market.*;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SectorSeedImportServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    private final InMemorySectorPort sectors = new InMemorySectorPort();
    private final InMemoryMappingPort mappings = new InMemoryMappingPort();
    private final InMemorySectorImportHistoryPort histories = new InMemorySectorImportHistoryPort();
    private final ResearchUseCases.SectorUseCase sectorUseCase = mock(ResearchUseCases.SectorUseCase.class);
    private final MarketCalendarPort calendar = mock(MarketCalendarPort.class);
    private final ResearchProperties properties = new ResearchProperties();
    private final SectorSeedImportService service = new SectorSeedImportService(sectors, mappings,
            histories, sectorUseCase, calendar, properties, OperationalMetricsPort.noop(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void importsSectorOnlyRow() {
        SectorImportHistory history = service.importCsv("""
                sectorCode,sectorName,sectorType,stockCode,source
                SEMICONDUCTOR,반도체,THEME,,
                """);

        assertThat(history.status()).isEqualTo(SectorImportStatus.SUCCESS);
        assertThat(history.importedSectorCount()).isEqualTo(1);
        assertThat(history.importedMappingCount()).isZero();
        assertThat(sectors.findBySectorCode("SEMICONDUCTOR"))
                .map(Sector::sectorName)
                .contains("반도체");
    }

    @Test
    void importsStockMappingRowAndPreventsDuplicateByPortUpsert() {
        String csv = """
                sectorCode,sectorName,sectorType,stockCode,source
                SEMICONDUCTOR,반도체,THEME,005930,CSV
                SEMICONDUCTOR,반도체,THEME,005930,CSV
                """;

        SectorImportHistory history = service.importCsv(csv);

        assertThat(history.status()).isEqualTo(SectorImportStatus.SUCCESS);
        assertThat(mappings.findBySectorCode("SEMICONDUCTOR")).hasSize(1);
    }

    @Test
    void invalidRowMakesPartialHistory() {
        SectorImportHistory history = service.importCsv("""
                sectorCode,sectorName,sectorType,stockCode,source
                SEMICONDUCTOR,반도체,THEME,005930,CSV
                ,이름없음,THEME,000000,CSV
                """);

        assertThat(history.status()).isEqualTo(SectorImportStatus.PARTIAL);
        assertThat(history.failureReason()).contains("invalidRows=1");
    }

    @Test
    void optionallyGeneratesSectorSnapshotAfterImport() {
        properties.setSectorImportAutoGenerateSnapshot(true);
        when(calendar.previousTradingDay(LocalDate.of(2026, 6, 15)))
                .thenReturn(LocalDate.of(2026, 6, 12));

        service.importCsv("""
                sectorCode,sectorName,sectorType,stockCode,source
                SEMICONDUCTOR,반도체,THEME,005930,CSV
                """);

        verify(sectorUseCase).generateSnapshots(LocalDate.of(2026, 6, 12));
    }

    @Test
    void hasNoBrokerOrOrderDependencySoImportCannotExecuteOrders() {
        assertThat(List.of(SectorSeedImportService.class.getDeclaredConstructors())
                .stream()
                .flatMap(constructor -> List.of(constructor.getParameterTypes()).stream())
                .map(Class::getName))
                .noneMatch(name -> name.contains("Broker") || name.contains("Order"));
    }

    private static class InMemorySectorPort implements SectorPort {
        private final Map<String, Sector> values = new HashMap<>();

        @Override
        public Sector save(Sector sector) {
            Sector saved = new Sector(1L, sector.sectorCode(), sector.sectorName(),
                    sector.sectorType(), sector.createdAt(), sector.updatedAt());
            values.put(saved.sectorCode(), saved);
            return saved;
        }

        @Override
        public List<Sector> findAll() {
            return new ArrayList<>(values.values());
        }

        @Override
        public Optional<Sector> findBySectorCode(String sectorCode) {
            return Optional.ofNullable(values.get(sectorCode));
        }
    }

    private static class InMemoryMappingPort implements StockSectorMappingPort {
        private final Map<String, StockSectorMapping> values = new HashMap<>();

        @Override
        public StockSectorMapping save(StockSectorMapping mapping) {
            String key = mapping.stockCode() + ":" + mapping.sectorCode();
            StockSectorMapping saved = new StockSectorMapping(1L, mapping.stockCode(),
                    mapping.sectorCode(), mapping.source(), mapping.createdAt(), mapping.updatedAt());
            values.put(key, saved);
            return saved;
        }

        @Override
        public List<StockSectorMapping> findBySectorCode(String sectorCode) {
            return values.values().stream()
                    .filter(value -> value.sectorCode().equals(sectorCode))
                    .toList();
        }

        @Override
        public List<StockSectorMapping> findByStockCode(String stockCode) {
            return values.values().stream()
                    .filter(value -> value.stockCode().equals(stockCode))
                    .toList();
        }

        @Override
        public List<StockSectorMapping> findAllMappings() {
            return new ArrayList<>(values.values());
        }
    }

    private static class InMemorySectorImportHistoryPort implements SectorImportHistoryPort {
        @Override
        public SectorImportHistory save(SectorImportHistory history) {
            return new SectorImportHistory(1L, history.status(), history.importedSectorCount(),
                    history.importedMappingCount(), history.failureReason(),
                    history.requestedAt(), history.completedAt());
        }

        @Override
        public List<SectorImportHistory> findRecentSectorImports(int limit) {
            return List.of();
        }
    }
}
