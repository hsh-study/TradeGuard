package seokhoon.trade.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.market.MarketIndex;
import seokhoon.trade.domain.market.Sector;
import seokhoon.trade.domain.market.SectorDailySnapshot;
import seokhoon.trade.domain.market.SectorType;
import seokhoon.trade.domain.market.StockSectorMapping;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MorningNoteServiceTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 15);
    private static final Instant NOW = Instant.parse("2026-06-14T23:10:00Z");

    private MorningNotePort notes;
    private StockPort stocks;
    private DailyPricePort prices;
    private IndicatorSnapshotPort indicators;
    private TradingSignalQueryPort signals;
    private LivePositionPort positions;
    private InvestmentThesisPort theses;
    private InvestmentCatalystPort catalysts;
    private MarketIndexPort marketIndices;
    private SectorPort sectors;
    private StockSectorMappingPort mappings;
    private SectorDailySnapshotPort sectorSnapshots;
    private ValuationSnapshotPort valuations;
    private SharesOutstandingSnapshotPort sharesOutstanding;
    private DartCorpCodeImportHistoryPort dartCorpCodeImportHistories;
    private SharesOutstandingImportHistoryPort sharesOutstandingImportHistories;
    private CatalystEvidencePort catalystEvidences;
    private DisclosureEvidenceImportHistoryPort disclosureEvidenceImportHistories;
    private MorningNoteService service;

    @BeforeEach
    void setUp() {
        notes = mock(MorningNotePort.class);
        stocks = mock(StockPort.class);
        prices = mock(DailyPricePort.class);
        indicators = mock(IndicatorSnapshotPort.class);
        signals = mock(TradingSignalQueryPort.class);
        positions = mock(LivePositionPort.class);
        theses = mock(InvestmentThesisPort.class);
        catalysts = mock(InvestmentCatalystPort.class);
        marketIndices = mock(MarketIndexPort.class);
        sectors = mock(SectorPort.class);
        mappings = mock(StockSectorMappingPort.class);
        sectorSnapshots = mock(SectorDailySnapshotPort.class);
        valuations = mock(ValuationSnapshotPort.class);
        sharesOutstanding = mock(SharesOutstandingSnapshotPort.class);
        dartCorpCodeImportHistories = mock(DartCorpCodeImportHistoryPort.class);
        sharesOutstandingImportHistories = mock(SharesOutstandingImportHistoryPort.class);
        catalystEvidences = mock(CatalystEvidencePort.class);
        disclosureEvidenceImportHistories = mock(DisclosureEvidenceImportHistoryPort.class);
        when(notes.save(any())).thenAnswer(invocation -> {
            MorningNote note = invocation.getArgument(0);
            return new MorningNote(1L, note.tradeDate(), note.marketSummary(), note.sectorSummary(),
                    note.portfolioImpactSummary(), note.watchlistSummary(), note.actionItems(), note.createdAt());
        });
        when(stocks.findAll()).thenReturn(List.of());
        when(positions.findOpenPositions()).thenReturn(List.of());
        when(signals.find(any())).thenReturn(List.of());
        when(theses.find(null, ThesisStatus.BROKEN)).thenReturn(List.of());
        when(catalysts.find(eq(null), any(), any(), eq(CatalystStatus.UPCOMING))).thenReturn(List.of());
        when(marketIndices.findByTradeDate(any())).thenReturn(List.of());
        when(sectors.findAll()).thenReturn(List.of());
        when(mappings.findByStockCode(any())).thenReturn(List.of());
        when(sectorSnapshots.findByTradeDate(any())).thenReturn(List.of());
        when(valuations.findLatestByStockCode(any(), any())).thenReturn(Optional.empty());
        when(sharesOutstanding.findLatestSharesByStockCode(any(), any())).thenReturn(Optional.empty());
        when(dartCorpCodeImportHistories.findAllCorpCodeImports()).thenReturn(List.of());
        when(sharesOutstandingImportHistories.findAllSharesOutstandingImports()).thenReturn(List.of());
        when(catalystEvidences.findRecent(anyInt())).thenReturn(List.of());
        when(catalystEvidences.findByCatalystId(anyLong())).thenReturn(List.of());
        when(disclosureEvidenceImportHistories.findRecentDisclosureImports(anyInt())).thenReturn(List.of());
        service = new MorningNoteService(
                notes, stocks, prices, indicators, signals, positions, theses, catalysts,
                date -> !date.getDayOfWeek().equals(DayOfWeek.SATURDAY)
                        && !date.getDayOfWeek().equals(DayOfWeek.SUNDAY),
                marketIndices, sectors, mappings, sectorSnapshots,
                null, null, null, null, null, null, valuations, sharesOutstanding,
                dartCorpCodeImportHistories, sharesOutstandingImportHistories,
                catalystEvidences, disclosureEvidenceImportHistories,
                OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void generatesMorningNoteWithUpcomingCatalystAndBrokenThesis() {
        when(catalysts.find(eq(null), any(), any(), eq(CatalystStatus.UPCOMING)))
                .thenReturn(List.of(catalyst()));
        when(theses.find(null, ThesisStatus.BROKEN)).thenReturn(List.of(brokenThesis()));

        MorningNote note = service.generate(TRADE_DATE);

        assertThat(note.actionItems()).contains("UPCOMING_CATALYST", "2Q earnings");
        assertThat(note.actionItems()).contains("BROKEN_THESIS", "margin declines");
    }

    @Test
    void includesIndicatorStatesForWatchlist() {
        when(stocks.findAll()).thenReturn(List.of(new Stock("005930", "Samsung", Market.KOSPI, true)));
        when(prices.findByStockCodeAndTradeDateBetween(eq("005930"), any(), eq(TRADE_DATE)))
                .thenReturn(List.of(price()));
        when(indicators.findByStockCodeAndTradeDateBetween(eq("005930"), any(), eq(TRADE_DATE)))
                .thenReturn(List.of(indicator()));
        when(sectors.findAll()).thenReturn(List.of(sector()));
        when(mappings.findByStockCode("005930")).thenReturn(List.of(mapping()));

        MorningNote note = service.generate(TRADE_DATE);

        assertThat(note.watchlistSummary())
                .contains("vsMA20=ABOVE", "vsMA60=ABOVE", "ma20>ma60=true")
                .contains("RSI=NEUTRAL", "MACD=BULLISH", "Bollinger=INSIDE")
                .contains("반도체(SEMICONDUCTOR)");
    }

    @Test
    void includesMarketIndexAndSectorSummary() {
        LocalDate previousTradingDay = LocalDate.of(2026, 6, 12);
        when(marketIndices.findByTradeDate(previousTradingDay))
                .thenReturn(List.of(index()));
        when(sectors.findAll()).thenReturn(List.of(sector(), new Sector(2L, "BIO", "바이오",
                SectorType.THEME, NOW, NOW)));
        when(sectorSnapshots.findByTradeDate(previousTradingDay))
                .thenReturn(List.of(
                        new SectorDailySnapshot(1L, "SEMICONDUCTOR", previousTradingDay,
                                bd("2.5000"), bd("2.3000"), bd("90000000000"),
                                2, 1, "005930", bd("4.1000"), NOW, NOW),
                        new SectorDailySnapshot(2L, "BIO", previousTradingDay,
                                bd("-1.1000"), bd("-0.8000"), bd("30000000000"),
                                1, 2, "068270", bd("1.0000"), NOW, NOW)
                ));

        MorningNote note = service.generate(TRADE_DATE);

        assertThat(note.marketSummary()).contains("KOSPI", "changeRate=1.2500%");
        assertThat(note.sectorSummary())
                .contains("상위 섹터", "하위 섹터")
                .contains("반도체(SEMICONDUCTOR)", "leader=005930");
    }

    @Test
    void warnsWhenIndicatorDataIsInsufficient() {
        when(stocks.findAll()).thenReturn(List.of(new Stock("005930", "Samsung", Market.KOSPI, true)));
        when(prices.findByStockCodeAndTradeDateBetween(any(), any(), any())).thenReturn(List.of(price()));
        when(indicators.findByStockCodeAndTradeDateBetween(any(), any(), any())).thenReturn(List.of());

        MorningNote note = service.generate(TRADE_DATE);

        assertThat(note.watchlistSummary()).contains("DATA_INSUFFICIENT");
        assertThat(note.actionItems()).contains("일봉/지표 보강 확인");
    }

    @Test
    void hasNoBrokerOrOrderDependencySoGenerationCannotExecuteOrders() {
        assertThat(List.of(MorningNoteService.class.getDeclaredConstructors())
                .stream()
                .flatMap(constructor -> List.of(constructor.getParameterTypes()).stream())
                .map(Class::getName))
                .noneMatch(name -> name.contains("Broker") || name.contains("Order"));
    }

    @Test
    void includesValuationActionItems() {
        when(stocks.findAll()).thenReturn(List.of(new Stock("005930", "Samsung", Market.KOSPI, true)));
        when(prices.findByStockCodeAndTradeDateBetween(any(), any(), any())).thenReturn(List.of(price()));
        when(indicators.findByStockCodeAndTradeDateBetween(any(), any(), any())).thenReturn(List.of(indicator()));
        when(sharesOutstanding.findLatestSharesByStockCode(eq("005930"), eq(TRADE_DATE)))
                .thenReturn(Optional.of(new SharesOutstandingSnapshot(1L, "005930", TRADE_DATE.minusDays(1),
                        bd("10"), SharesOutstandingSource.MANUAL, NOW, NOW)));
        when(valuations.findLatestByStockCode(eq("005930"), eq(TRADE_DATE)))
                .thenReturn(Optional.of(new ValuationSnapshot(1L, "005930", TRADE_DATE.minusDays(1),
                        bd("1000"), null, bd("3.5000"), bd("6.0000"),
                        bd("-1.0000"), bd("30.0000"), bd("100.0000"),
                        ValuationSnapshotSource.AUTO, NOW, NOW)));
        when(dartCorpCodeImportHistories.findAllCorpCodeImports())
                .thenReturn(List.of(new DartCorpCodeImportHistory(1L,
                        DartCorpCodeImportStatus.SUCCESS, 2, 1, null, NOW, NOW)));
        when(sharesOutstandingImportHistories.findAllSharesOutstandingImports())
                .thenReturn(List.of(new SharesOutstandingImportHistory(1L,
                        SharesOutstandingImportStatus.SUCCESS, 1, null, NOW, NOW)));
        when(catalystEvidences.findRecent(anyInt()))
                .thenReturn(List.of(new CatalystEvidence(1L, 1L, "005930",
                        CatalystEvidenceType.POST_EARNINGS_REVIEW, "review evidence", "summary",
                        "TradeGuard", null, NOW, EvidenceConfidence.HIGH,
                        EvidenceCreatedBy.SYSTEM, EvidenceStatus.ACTIVE, NOW, NOW)));
        when(disclosureEvidenceImportHistories.findRecentDisclosureImports(anyInt()))
                .thenReturn(List.of(new DisclosureEvidenceImportHistory(1L, DisclosureProvider.DART,
                        "005930", TRADE_DATE.minusDays(7), TRADE_DATE,
                        DisclosureEvidenceImportStatus.FAILED, 0, "provider failed", NOW, NOW)));

        MorningNote note = service.generate(TRADE_DATE);

        assertThat(note.actionItems())
                .contains("VALUATION_AUTO_GENERATED")
                .contains("VALUATION_NEGATIVE_EARNINGS")
                .contains("VALUATION_OVERVALUED_WARNING")
                .contains("DART_CORP_MAPPING_IMPORTED")
                .contains("SHARES_OUTSTANDING_IMPORTED")
                .contains("NEW_DISCLOSURE_EVIDENCE")
                .contains("POST_EARNINGS_REVIEW_EVIDENCE")
                .contains("DISCLOSURE_IMPORT_FAILED");
    }

    private static DailyPrice price() {
        return new DailyPrice("005930", TRADE_DATE.minusDays(1),
                bd("100"), bd("115"), bd("95"), bd("110"), 1000, bd("100000"));
    }

    private static IndicatorSnapshot indicator() {
        return new IndicatorSnapshot("005930", TRADE_DATE.minusDays(1),
                bd("108"), bd("105"), bd("100"), bd("55"),
                bd("2"), bd("1"), bd("1"), bd("120"), bd("105"), bd("90"));
    }

    private static MarketIndex index() {
        return new MarketIndex(1L, "KOSPI", "KOSPI", LocalDate.of(2026, 6, 12),
                bd("2800"), bd("1.2500"), bd("9000000000000"), NOW, NOW);
    }

    private static Sector sector() {
        return new Sector(1L, "SEMICONDUCTOR", "반도체", SectorType.THEME, NOW, NOW);
    }

    private static StockSectorMapping mapping() {
        return new StockSectorMapping(1L, "005930", "SEMICONDUCTOR", "MANUAL", NOW, NOW);
    }

    private static InvestmentCatalyst catalyst() {
        return new InvestmentCatalyst(1L, "005930", "2Q earnings", CatalystType.EARNINGS,
                TRADE_DATE.plusDays(5), CatalystImportance.HIGH, CatalystStatus.UPCOMING,
                null, null, NOW, NOW);
    }

    private static InvestmentThesis brokenThesis() {
        return new InvestmentThesis(1L, "005930", "memory recovery", "cycle improves",
                "margin declines", bd("90000"), "close below MA60", 70,
                ThesisStatus.BROKEN, NOW, NOW);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
