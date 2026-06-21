package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ImportDisclosureActualEvidenceUseCase;
import seokhoon.trade.application.port.in.DartCorpCodeImportUseCase;
import seokhoon.trade.application.port.out.DartCorpMappingPort;
import seokhoon.trade.application.port.out.StockPort;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class WatchlistMaterialServiceTest {
    private static final LocalDate FROM = LocalDate.of(2026, 5, 20);
    private static final LocalDate TO = LocalDate.of(2026, 6, 20);

    @Test
    void collectsOnlyForRegisteredStockAndReturnsSafeSummary() {
        StockPort stocks = mock(StockPort.class);
        ImportDisclosureActualEvidenceUseCase disclosures = mock(ImportDisclosureActualEvidenceUseCase.class);
        when(stocks.findByStockCode("005930"))
                .thenReturn(Optional.of(new Stock("005930", "삼성전자", Market.KOSPI, true)));
        when(disclosures.importStock("005930", FROM, TO)).thenReturn(new DisclosureEvidenceImportHistory(
                1L, DisclosureProvider.DART, "005930", FROM, TO,
                DisclosureEvidenceImportStatus.SUCCESS, 2, "private failure detail",
                Instant.parse("2026-06-20T00:00:00Z"), Instant.parse("2026-06-20T00:00:01Z")));

        DartCorpMappingPort mappings = mock(DartCorpMappingPort.class);
        when(mappings.findByStockCode("005930")).thenReturn(Optional.of(mock(DartCorpMapping.class)));
        var result = service(stocks, disclosures, mappings).collect("005930", FROM, TO);

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.message()).isEqualTo("COLLECTION_COMPLETED");
        assertThat(result.toString()).doesNotContain("private failure detail");
        verify(disclosures).importStock("005930", FROM, TO);
    }

    @Test
    void doesNotCallProviderForUnregisteredStock() {
        StockPort stocks = mock(StockPort.class);
        ImportDisclosureActualEvidenceUseCase disclosures = mock(ImportDisclosureActualEvidenceUseCase.class);
        when(stocks.findByStockCode("005930")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(stocks, disclosures, mock(DartCorpMappingPort.class))
                .collect("005930", FROM, TO)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(disclosures);
    }

    @Test
    void mapsEvidenceWithoutSensitiveSourceMetadata() {
        StockPort stocks = mock(StockPort.class);
        ImportDisclosureActualEvidenceUseCase disclosures = mock(ImportDisclosureActualEvidenceUseCase.class);
        when(stocks.findByStockCode("005930"))
                .thenReturn(Optional.of(new Stock("005930", "삼성전자", Market.KOSPI, true)));
        CatalystEvidence evidence = new CatalystEvidence(1L, null, "005930",
                CatalystEvidenceType.DART_DISCLOSURE, "공급계약", "계약 체결",
                "DART", "https://sensitive.example", Instant.parse("2026-06-19T01:00:00Z"),
                "receipt-secret", "계약", CatalystType.ORDER_CONTRACT, CatalystImportance.HIGH,
                "raw", EvidenceConfidence.HIGH, EvidenceCreatedBy.PROVIDER, EvidenceStatus.ACTIVE,
                Instant.parse("2026-06-19T01:00:00Z"), Instant.parse("2026-06-19T01:00:00Z"));
        when(disclosures.findEvidences("005930", FROM, TO)).thenReturn(List.of(evidence));

        var items = service(stocks, disclosures, mock(DartCorpMappingPort.class)).find("005930", FROM, TO);

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.title()).isEqualTo("공급계약");
            assertThat(item.toString()).doesNotContain("sensitive.example", "receipt-secret");
        });
    }

    @Test
    void importsDartCorpCodesBeforeCollectionWhenMappingIsMissing() {
        StockPort stocks = mock(StockPort.class);
        ImportDisclosureActualEvidenceUseCase disclosures = mock(ImportDisclosureActualEvidenceUseCase.class);
        DartCorpMappingPort mappings = mock(DartCorpMappingPort.class);
        DartCorpCodeImportUseCase corpCodes = mock(DartCorpCodeImportUseCase.class);
        when(stocks.findByStockCode("005930"))
                .thenReturn(Optional.of(new Stock("005930", "삼성전자", Market.KOSPI, true)));
        when(disclosures.importStock("005930", FROM, TO)).thenReturn(new DisclosureEvidenceImportHistory(
                1L, DisclosureProvider.DART, "005930", FROM, TO,
                DisclosureEvidenceImportStatus.SUCCESS, 1, null,
                Instant.parse("2026-06-20T00:00:00Z"), Instant.parse("2026-06-20T00:00:01Z")));

        new WatchlistMaterialService(stocks, disclosures, mappings, corpCodes)
                .collect("005930", FROM, TO);

        var ordered = inOrder(corpCodes, disclosures);
        ordered.verify(corpCodes).importCorpCodes();
        ordered.verify(disclosures).importStock("005930", FROM, TO);
    }

    private static WatchlistMaterialService service(StockPort stocks,
            ImportDisclosureActualEvidenceUseCase disclosures, DartCorpMappingPort mappings) {
        return new WatchlistMaterialService(stocks, disclosures, mappings,
                mock(DartCorpCodeImportUseCase.class));
    }
}
