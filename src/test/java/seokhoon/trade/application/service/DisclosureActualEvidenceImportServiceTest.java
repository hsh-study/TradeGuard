package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.DisclosureActualProviderProperties;
import seokhoon.trade.domain.research.*;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DisclosureActualEvidenceImportServiceTest {
    private static final LocalDate FROM=LocalDate.of(2026,6,1), TO=LocalDate.of(2026,6,15);
    private static final Instant NOW=Instant.parse("2026-06-15T00:00:00Z");

    @Test void disabledProviderRecordsSkippedWithoutCallingProviderOrOrders() {
        DisclosureActualProviderPort provider=mock(DisclosureActualProviderPort.class);
        InMemoryHistory histories=new InMemoryHistory();
        var service=service(provider,histories,new CatalystEvidenceServiceTest.InMemoryEvidencePort(),new DisclosureActualProviderProperties());
        var result=service.importStock("005930",FROM,TO);
        assertThat(result.status()).isEqualTo(DisclosureEvidenceImportStatus.SKIPPED);
        verifyNoInteractions(provider);
    }

    @Test void importsMetadataAndDeduplicatesReceiptNumber() {
        DisclosureActualProviderProperties properties=new DisclosureActualProviderProperties();properties.setEnabled(true);
        DisclosureActualRecord record=new DisclosureActualRecord("005930",TO,null,"유상증자 결정","HIGH_RISK_DISCLOSURE",
                DisclosureProvider.DART,"https://dart.fss.or.kr/view","202606150001",CatalystType.DISCLOSURE,
                CatalystImportance.HIGH,"I");
        DisclosureActualProviderPort provider=(stock,from,to)->List.of(record,record);
        CatalystEvidenceServiceTest.InMemoryEvidencePort evidencePort=new CatalystEvidenceServiceTest.InMemoryEvidencePort();
        var service=service(provider,new InMemoryHistory(),evidencePort,properties);
        var result=service.importStock("005930",FROM,TO);
        assertThat(result.status()).isEqualTo(DisclosureEvidenceImportStatus.SUCCESS);
        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(evidencePort.findEvidenceByStockCode("005930")).singleElement().satisfies(value->{
            assertThat(value.receiptNo()).isEqualTo("202606150001");
            assertThat(value.importance()).isEqualTo(CatalystImportance.HIGH);
            assertThat(value.summary()).doesNotContain("<html", "attachment", "rawBody");
        });
    }

    @Test void sanitizesProviderFailureWithoutLeakingResponseOrApiKey() {
        DisclosureActualProviderProperties properties=new DisclosureActualProviderProperties();properties.setEnabled(true);
        DisclosureActualProviderPort provider=(stock,from,to)->{throw new IllegalStateException("apiKey=secret raw-response=<html>private</html>");};
        var result=service(provider,new InMemoryHistory(),new CatalystEvidenceServiceTest.InMemoryEvidencePort(),properties)
                .importStock("005930",FROM,TO);
        assertThat(result.status()).isEqualTo(DisclosureEvidenceImportStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("IllegalStateException")
                .doesNotContain("secret","raw-response","html");
    }

    private static DisclosureActualEvidenceImportService service(DisclosureActualProviderPort provider,
            InMemoryHistory histories,CatalystEvidenceServiceTest.InMemoryEvidencePort evidencePort,
            DisclosureActualProviderProperties properties) {
        CatalystEvidenceService evidenceService=new CatalystEvidenceService(evidencePort,OperationalMetricsPort.noop(),Clock.fixed(NOW,ZoneOffset.UTC));
        return new DisclosureActualEvidenceImportService(provider,histories,evidencePort,evidenceService,
                mock(InvestmentCatalystPort.class),mock(StockPort.class),mock(LivePositionPort.class),properties,
                OperationalMetricsPort.noop(),Clock.fixed(NOW,ZoneOffset.UTC));
    }
    private static class InMemoryHistory implements DisclosureEvidenceImportHistoryPort {
        final List<DisclosureEvidenceImportHistory> values=new ArrayList<>();
        public DisclosureEvidenceImportHistory save(DisclosureEvidenceImportHistory value){values.add(value);return value;}
        public List<DisclosureEvidenceImportHistory> findRecentDisclosureImports(int limit){return values.stream().limit(limit).toList();}
    }
}
