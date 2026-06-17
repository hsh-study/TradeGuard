package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.AnalyzeEarningsUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.DartProperties;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DartFinancialImportServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Test
    void upsertsQuarterlyFinancialAndRunsAutoAnalyze() {
        InMemoryFinancialPort financials = new InMemoryFinancialPort();
        InMemoryHistoryPort histories = new InMemoryHistoryPort();
        CountingAnalyzeUseCase analyzer = new CountingAnalyzeUseCase();
        DartFinancialImportService service = service(mappingPort(true), provider(false, completeAccounts()),
                financials, histories, analyzer, stockPort());

        DartFinancialImportHistory history = service.importStock("005930", 2026, "11013");

        assertThat(history.status()).isEqualTo(DartFinancialImportStatus.SUCCESS);
        assertThat(financials.values).hasSize(1);
        assertThat(financials.values.get(0).fiscalQuarter()).isEqualTo(1);
        assertThat(financials.values.get(0).freeCashFlow()).isNull();
        assertThat(analyzer.count).isEqualTo(1);
    }

    @Test
    void recordsSkippedWhenMappingIsMissing() {
        DartFinancialImportService service = service(mappingPort(false), provider(false, completeAccounts()),
                new InMemoryFinancialPort(), new InMemoryHistoryPort(),
                new CountingAnalyzeUseCase(), stockPort());

        DartFinancialImportHistory history = service.importStock("005930", 2026, "11013");

        assertThat(history.status()).isEqualTo(DartFinancialImportStatus.SKIPPED);
        assertThat(history.failureReason()).contains("mapping");
    }

    @Test
    void recordsFailedWhenProviderFails() {
        DartFinancialImportService service = service(mappingPort(true), provider(true, completeAccounts()),
                new InMemoryFinancialPort(), new InMemoryHistoryPort(),
                new CountingAnalyzeUseCase(), stockPort());

        DartFinancialImportHistory history = service.importStock("005930", 2026, "11013");

        assertThat(history.status()).isEqualTo(DartFinancialImportStatus.FAILED);
        assertThat(history.failureReason()).contains("provider failed");
    }

    @Test
    void redactsApiKeyFromFailureHistory() {
        String secret = "secret-dart-key";
        DartFinancialImportService service = service(mappingPort(true),
                (corpCode, fiscalYear, reportCode) -> {
                    throw new IllegalStateException("provider failed for " + secret);
                }, new InMemoryFinancialPort(), new InMemoryHistoryPort(),
                new CountingAnalyzeUseCase(), stockPort(), secret);

        DartFinancialImportHistory history = service.importStock("005930", 2026, "11013");

        assertThat(history.status()).isEqualTo(DartFinancialImportStatus.FAILED);
        assertThat(history.failureReason()).doesNotContain(secret);
        assertThat(history.failureReason()).contains("[REDACTED]");
    }

    @Test
    void recordsPartialWhenRequiredAccountIsMissing() {
        DartFinancialImportService service = service(mappingPort(true), provider(false, List.of(
                new DartFinancialAccount("매출액", new BigDecimal("1000"))
        )), new InMemoryFinancialPort(), new InMemoryHistoryPort(),
                new CountingAnalyzeUseCase(), stockPort());

        DartFinancialImportHistory history = service.importStock("005930", 2026, "11013");

        assertThat(history.status()).isEqualTo(DartFinancialImportStatus.PARTIAL);
        assertThat(history.importedQuarterlyFinancialCount()).isZero();
    }

    private static DartFinancialImportService service(
            DartCorpMappingPort mappingPort,
            DartFinancialProviderPort providerPort,
            InMemoryFinancialPort financialPort,
            InMemoryHistoryPort historyPort,
            CountingAnalyzeUseCase analyzer,
            StockPort stockPort
    ) {
        DartProperties properties = new DartProperties();
        properties.setImportAutoAnalyze(true);
        properties.setImportLookbackQuarters(2);
        return new DartFinancialImportService(mappingPort, providerPort, financialPort,
                historyPort, analyzer, stockPort, properties, OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static DartFinancialImportService service(
            DartCorpMappingPort mappingPort,
            DartFinancialProviderPort providerPort,
            InMemoryFinancialPort financialPort,
            InMemoryHistoryPort historyPort,
            CountingAnalyzeUseCase analyzer,
            StockPort stockPort,
            String apiKey
    ) {
        DartProperties properties = new DartProperties();
        properties.setApiKey(apiKey);
        properties.setImportAutoAnalyze(true);
        properties.setImportLookbackQuarters(2);
        return new DartFinancialImportService(mappingPort, providerPort, financialPort,
                historyPort, analyzer, stockPort, properties, OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static DartCorpMappingPort mappingPort(boolean present) {
        return new DartCorpMappingPort() {
            @Override public DartCorpMapping save(DartCorpMapping value) { return value; }
            @Override
            public Optional<DartCorpMapping> findByStockCode(String stockCode) {
                return present
                        ? Optional.of(new DartCorpMapping(1L, stockCode, "00126380",
                        "삼성전자", Market.KOSPI, NOW, NOW))
                        : Optional.empty();
            }
            @Override public List<DartCorpMapping> findAll() { return List.of(); }
        };
    }

    private static DartFinancialProviderPort provider(boolean fail, List<DartFinancialAccount> accounts) {
        return (corpCode, fiscalYear, reportCode) -> {
            if (fail) {
                throw new IllegalStateException("provider failed");
            }
            return new DartFinancialStatement(corpCode, fiscalYear, reportCode, accounts);
        };
    }

    private static List<DartFinancialAccount> completeAccounts() {
        return List.of(
                new DartFinancialAccount("매출액", new BigDecimal("1000")),
                new DartFinancialAccount("영업이익", new BigDecimal("150")),
                new DartFinancialAccount("당기순이익", new BigDecimal("100")),
                new DartFinancialAccount("자산총계", new BigDecimal("5000")),
                new DartFinancialAccount("부채총계", new BigDecimal("2000")),
                new DartFinancialAccount("자본총계", new BigDecimal("3000")),
                new DartFinancialAccount("영업활동현금흐름", new BigDecimal("120"))
        );
    }

    private static StockPort stockPort() {
        return new StockPort() {
            @Override public Stock save(Stock stock) { return stock; }
            @Override public List<Stock> findAll() { return List.of(new Stock("005930", "삼성전자", Market.KOSPI, true)); }
        };
    }

    private static class InMemoryFinancialPort implements QuarterlyFinancialPort {
        private final List<QuarterlyFinancial> values = new ArrayList<>();
        @Override public List<QuarterlyFinancial> saveAll(List<QuarterlyFinancial> values) { this.values.addAll(values); return values; }
        @Override public List<QuarterlyFinancial> findRecentQuarters(String stockCode, int limit) { return values; }
        @Override public Optional<QuarterlyFinancial> findByStockCodeAndQuarter(String stockCode, int fiscalYear, int fiscalQuarter) { return Optional.empty(); }
    }

    private static class InMemoryHistoryPort implements DartFinancialImportHistoryPort {
        private final List<DartFinancialImportHistory> values = new ArrayList<>();
        @Override public DartFinancialImportHistory save(DartFinancialImportHistory value) { values.add(value); return value; }
        @Override public List<DartFinancialImportHistory> findHistoriesByStockCode(String stockCode) { return values; }
    }

    private static class CountingAnalyzeUseCase implements AnalyzeEarningsUseCase {
        private int count;
        @Override public EarningsAnalysisSnapshot analyzeStock(String stockCode, LocalDate baseDate) { count++; return null; }
        @Override public List<EarningsAnalysisSnapshot> analyzeStocks(List<String> stockCodes, LocalDate baseDate) { return List.of(); }
    }
}
