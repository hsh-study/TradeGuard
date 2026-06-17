package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.GenerateValuationSnapshotUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.ResearchProperties;
import seokhoon.trade.domain.research.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SharesOutstandingImportServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Test
    void importsSharesOutstandingCsv() {
        InMemorySharesPort shares = new InMemorySharesPort();
        SharesOutstandingImportHistory history = service(shares, new CountingValuationUseCase(), false)
                .importCsv("stockCode,baseDate,sharesOutstanding,source\n005930,2026-06-15,10,MANUAL\n");

        assertThat(history.status()).isEqualTo(SharesOutstandingImportStatus.SUCCESS);
        assertThat(history.importedCount()).isEqualTo(1);
        assertThat(shares.values).hasSize(1);
    }

    @Test
    void recordsPartialForInvalidCsvRows() {
        SharesOutstandingImportHistory history = service(new InMemorySharesPort(),
                new CountingValuationUseCase(), false)
                .importCsv("""
                        stockCode,baseDate,sharesOutstanding,source
                        005930,2026-06-15,10,MANUAL
                        000660,wrong-date,20,MANUAL
                        """);

        assertThat(history.status()).isEqualTo(SharesOutstandingImportStatus.PARTIAL);
        assertThat(history.importedCount()).isEqualTo(1);
        assertThat(history.failureReason()).contains("invalidRows=1");
    }

    @Test
    void optionallyGeneratesValuationAfterImport() {
        CountingValuationUseCase valuation = new CountingValuationUseCase();
        SharesOutstandingImportHistory history = service(new InMemorySharesPort(), valuation, true)
                .importCsv("stockCode,baseDate,sharesOutstanding,source\n005930,2026-06-15,10,MANUAL\n");

        assertThat(history.status()).isEqualTo(SharesOutstandingImportStatus.SUCCESS);
        assertThat(valuation.count).isEqualTo(1);
        assertThat(valuation.baseDate).isEqualTo(LocalDate.of(2026, 6, 15));
    }

    private static SharesOutstandingImportService service(
            InMemorySharesPort shares,
            CountingValuationUseCase valuation,
            boolean autoGenerate
    ) {
        ResearchProperties properties = new ResearchProperties();
        properties.setSharesOutstandingImportAutoGenerateValuation(autoGenerate);
        return new SharesOutstandingImportService(shares, new InMemoryHistoryPort(),
                valuation, properties, OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static class InMemorySharesPort implements SharesOutstandingSnapshotPort {
        private final List<SharesOutstandingSnapshot> values = new ArrayList<>();
        @Override public SharesOutstandingSnapshot save(SharesOutstandingSnapshot value) {
            values.add(value);
            return value;
        }
        @Override public Optional<SharesOutstandingSnapshot> findLatestSharesByStockCode(String stockCode, LocalDate baseDate) {
            return values.stream().findFirst();
        }
        @Override public List<SharesOutstandingSnapshot> findSharesByStockCode(String stockCode) {
            return values;
        }
    }

    private static class InMemoryHistoryPort implements SharesOutstandingImportHistoryPort {
        private final List<SharesOutstandingImportHistory> values = new ArrayList<>();
        @Override public SharesOutstandingImportHistory save(SharesOutstandingImportHistory value) {
            values.add(value);
            return value;
        }
        @Override public List<SharesOutstandingImportHistory> findAllSharesOutstandingImports() {
            return values;
        }
    }

    private static class CountingValuationUseCase implements GenerateValuationSnapshotUseCase {
        private int count;
        private LocalDate baseDate;
        @Override public ValuationGenerationResult generate(String stockCode, LocalDate baseDate) {
            count++;
            this.baseDate = baseDate;
            return null;
        }
        @Override public List<ValuationGenerationResult> generateBatch(List<String> stockCodes, LocalDate baseDate) {
            return List.of();
        }
        @Override public List<ValuationGenerationResult> generateWatchlist(LocalDate baseDate) {
            return List.of();
        }
    }
}
