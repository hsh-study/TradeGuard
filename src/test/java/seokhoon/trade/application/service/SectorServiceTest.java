package seokhoon.trade.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ResearchUseCases.AddSectorStockCommand;
import seokhoon.trade.application.port.in.ResearchUseCases.CreateSectorCommand;
import seokhoon.trade.application.port.in.ResearchUseCases.SectorSnapshotGenerationResult;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.market.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SectorServiceTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 12);
    private static final LocalDate PREVIOUS_DATE = LocalDate.of(2026, 6, 11);
    private static final Instant NOW = Instant.parse("2026-06-12T00:00:00Z");

    private SectorPort sectors;
    private StockSectorMappingPort mappings;
    private SectorDailySnapshotPort snapshots;
    private DailyPricePort prices;
    private SectorService service;

    @BeforeEach
    void setUp() {
        sectors = mock(SectorPort.class);
        mappings = mock(StockSectorMappingPort.class);
        snapshots = mock(SectorDailySnapshotPort.class);
        prices = mock(DailyPricePort.class);
        when(snapshots.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new SectorService(
                sectors,
                mappings,
                snapshots,
                prices,
                date -> !date.getDayOfWeek().equals(DayOfWeek.SATURDAY)
                        && !date.getDayOfWeek().equals(DayOfWeek.SUNDAY),
                OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsSectorAndStockMapping() {
        Sector saved = new Sector(1L, "SEMICONDUCTOR", "반도체", SectorType.THEME, NOW, NOW);
        when(sectors.save(any())).thenReturn(saved);
        when(sectors.findBySectorCode("SEMICONDUCTOR")).thenReturn(Optional.of(saved));
        when(mappings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Sector sector = service.create(new CreateSectorCommand("SEMICONDUCTOR", "반도체", SectorType.THEME));
        StockSectorMapping mapping = service.addStock("SEMICONDUCTOR",
                new AddSectorStockCommand("005930", "MANUAL"));

        assertThat(sector.sectorCode()).isEqualTo("SEMICONDUCTOR");
        assertThat(mapping.stockCode()).isEqualTo("005930");
        assertThat(mapping.source()).isEqualTo("MANUAL");
    }

    @Test
    void calculatesSectorSnapshotAndLeadingStock() {
        when(sectors.findAll()).thenReturn(List.of(sector()));
        when(mappings.findBySectorCode("SEMICONDUCTOR"))
                .thenReturn(List.of(mapping("005930"), mapping("000660"), mapping("035420")));
        price("005930", PREVIOUS_DATE, "100", "1000");
        price("005930", TRADE_DATE, "110", "3000");
        price("000660", PREVIOUS_DATE, "100", "1000");
        price("000660", TRADE_DATE, "105", "2000");
        price("035420", PREVIOUS_DATE, "100", "1000");
        price("035420", TRADE_DATE, "90", "4000");

        SectorSnapshotGenerationResult result = service.generateSnapshots(TRADE_DATE);

        assertThat(result.generatedCount()).isEqualTo(1);
        verify(snapshots).save(argThat(snapshot ->
                snapshot.averageChangeRate().compareTo(new BigDecimal("1.6667")) == 0
                        && snapshot.medianChangeRate().compareTo(new BigDecimal("5.0000")) == 0
                        && snapshot.totalTradingValue().compareTo(new BigDecimal("9000")) == 0
                        && snapshot.risingStockCount() == 2
                        && snapshot.fallingStockCount() == 1
                        && snapshot.leadingStockCode().equals("005930")
                        && snapshot.leadingStockChangeRate().compareTo(new BigDecimal("10.0000")) == 0
        ));
    }

    @Test
    void storesDataInsufficientSnapshotWhenPricesAreMissing() {
        when(sectors.findAll()).thenReturn(List.of(sector()));
        when(mappings.findBySectorCode("SEMICONDUCTOR")).thenReturn(List.of(mapping("005930")));

        SectorSnapshotGenerationResult result = service.generateSnapshots(TRADE_DATE);

        assertThat(result.dataInsufficientCount()).isEqualTo(1);
        verify(snapshots).save(argThat(SectorDailySnapshot::dataInsufficient));
    }

    @Test
    void hasNoBrokerOrOrderDependencySoSnapshotsCannotExecuteOrders() {
        assertThat(List.of(SectorService.class.getDeclaredConstructors())
                .stream()
                .flatMap(constructor -> List.of(constructor.getParameterTypes()).stream())
                .map(Class::getName))
                .noneMatch(name -> name.contains("Broker") || name.contains("Order"));
    }

    private void price(String stockCode, LocalDate tradeDate, String closePrice, String tradingValue) {
        when(prices.findByStockCodeAndTradeDateBetween(eq(stockCode), any(), eq(tradeDate)))
                .thenReturn(List.of(new DailyPrice(stockCode, tradeDate,
                        bd("100"), bd("110"), bd("90"), bd(closePrice), 1000, bd(tradingValue))));
    }

    private static Sector sector() {
        return new Sector(1L, "SEMICONDUCTOR", "반도체", SectorType.THEME, NOW, NOW);
    }

    private static StockSectorMapping mapping(String stockCode) {
        return new StockSectorMapping(1L, stockCode, "SEMICONDUCTOR", "MANUAL", NOW, NOW);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
