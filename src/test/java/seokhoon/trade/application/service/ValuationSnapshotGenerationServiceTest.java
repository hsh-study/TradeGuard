package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.AnalyzeEarningsUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.ResearchProperties;
import seokhoon.trade.domain.market.DailyPrice;
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

class ValuationSnapshotGenerationServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 6, 15);

    @Test
    void calculatesMarketCapPerPbrPsrAndPerShareValues() {
        InMemoryValuationPort valuations = new InMemoryValuationPort();
        CountingAnalyzeUseCase analyzer = new CountingAnalyzeUseCase();
        ValuationSnapshotGenerationService service = service(
                List.of(price("100")),
                List.of(financial("1000", "100", "300")),
                Optional.of(shares("10")),
                valuations,
                analyzer
        );

        ValuationGenerationResult result = service.generate("005930", BASE_DATE);

        assertThat(result.status()).isEqualTo(ValuationGenerationStatus.GENERATED);
        ValuationSnapshot snapshot = result.snapshot();
        assertThat(snapshot.marketCap()).isEqualByComparingTo("1000.0000");
        assertThat(snapshot.eps()).isEqualByComparingTo("10.0000");
        assertThat(snapshot.bps()).isEqualByComparingTo("30.0000");
        assertThat(snapshot.salesPerShare()).isEqualByComparingTo("100.0000");
        assertThat(snapshot.per()).isEqualByComparingTo("10.0000");
        assertThat(snapshot.pbr()).isEqualByComparingTo("3.3333");
        assertThat(snapshot.psr()).isEqualByComparingTo("1.0000");
        assertThat(snapshot.source()).isEqualTo(ValuationSnapshotSource.AUTO);
        assertThat(analyzer.count).isEqualTo(1);
    }

    @Test
    void leavesPerNullWhenNetIncomeIsNegative() {
        ValuationSnapshotGenerationService service = service(
                List.of(price("100")),
                List.of(financial("1000", "-10", "300")),
                Optional.of(shares("10")),
                new InMemoryValuationPort(),
                new CountingAnalyzeUseCase()
        );

        ValuationGenerationResult result = service.generate("005930", BASE_DATE);

        assertThat(result.snapshot().per()).isNull();
        assertThat(result.reasons()).contains("NEGATIVE_EARNINGS");
    }

    @Test
    void returnsDataInsufficientWhenSharesOutstandingIsMissing() {
        ValuationGenerationResult result = service(
                List.of(price("100")),
                List.of(financial("1000", "100", "300")),
                Optional.empty(),
                new InMemoryValuationPort(),
                new CountingAnalyzeUseCase()
        ).generate("005930", BASE_DATE);

        assertThat(result.status()).isEqualTo(ValuationGenerationStatus.DATA_INSUFFICIENT);
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("SHARES_OUTSTANDING_REQUIRED"));
    }

    @Test
    void returnsDataInsufficientWhenDailyPriceIsMissing() {
        ValuationGenerationResult result = service(
                List.of(),
                List.of(financial("1000", "100", "300")),
                Optional.of(shares("10")),
                new InMemoryValuationPort(),
                new CountingAnalyzeUseCase()
        ).generate("005930", BASE_DATE);

        assertThat(result.status()).isEqualTo(ValuationGenerationStatus.DATA_INSUFFICIENT);
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("daily price"));
    }

    @Test
    void savesAndLoadsSharesOutstandingSnapshots() {
        InMemorySharesPort sharesPort = new InMemorySharesPort(Optional.empty());
        EarningsDataService service = new EarningsDataService(
                new EmptyFinancialPort(),
                new InMemoryValuationPort(),
                sharesPort,
                OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        SharesOutstandingSnapshot saved = service.saveSharesOutstanding(
                new seokhoon.trade.application.port.in.ResearchUseCases.SaveSharesOutstandingCommand(
                        "005930", BASE_DATE, new BigDecimal("10"), SharesOutstandingSource.MANUAL));

        assertThat(saved.sharesOutstanding()).isEqualByComparingTo("10");
        assertThat(service.findSharesOutstanding("005930")).containsExactly(saved);
    }

    private static ValuationSnapshotGenerationService service(
            List<DailyPrice> prices,
            List<QuarterlyFinancial> financials,
            Optional<SharesOutstandingSnapshot> shares,
            InMemoryValuationPort valuations,
            CountingAnalyzeUseCase analyzer
    ) {
        ResearchProperties properties = new ResearchProperties();
        properties.setValuationAutoSnapshotAutoAnalyze(true);
        return new ValuationSnapshotGenerationService(
                new StaticDailyPricePort(prices),
                new StaticFinancialPort(financials),
                new InMemorySharesPort(shares),
                valuations,
                new StaticStockPort(),
                analyzer,
                properties,
                OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private static DailyPrice price(String close) {
        return new DailyPrice("005930", BASE_DATE, new BigDecimal(close),
                new BigDecimal(close), new BigDecimal(close), new BigDecimal(close),
                1000, new BigDecimal("100000"));
    }

    private static QuarterlyFinancial financial(String revenue, String netIncome, String equity) {
        return new QuarterlyFinancial(1L, "005930", 2026, 1,
                new BigDecimal(revenue), new BigDecimal("50"), new BigDecimal(netIncome),
                new BigDecimal("5000"), new BigDecimal("2000"), new BigDecimal(equity),
                new BigDecimal("100"), new BigDecimal("50"), NOW, NOW);
    }

    private static SharesOutstandingSnapshot shares(String value) {
        return new SharesOutstandingSnapshot(1L, "005930", BASE_DATE,
                new BigDecimal(value), SharesOutstandingSource.MANUAL, NOW, NOW);
    }

    private static class InMemoryValuationPort implements ValuationSnapshotPort {
        private final List<ValuationSnapshot> values = new ArrayList<>();
        @Override public ValuationSnapshot save(ValuationSnapshot value) { values.add(value); return value; }
        @Override public Optional<ValuationSnapshot> findLatestByStockCode(String stockCode, LocalDate baseDate) {
            return values.stream().findFirst();
        }
    }

    private record StaticDailyPricePort(List<DailyPrice> values) implements DailyPricePort {
        @Override public List<DailyPrice> saveAll(List<DailyPrice> dailyPrices) {
            return dailyPrices;
        }

        @Override public List<DailyPrice> findByStockCodeAndTradeDateBetween(
                String stockCode,
                LocalDate from,
                LocalDate to
        ) {
            return values;
        }
    }

    private static class InMemorySharesPort implements SharesOutstandingSnapshotPort {
        private final List<SharesOutstandingSnapshot> values = new ArrayList<>();

        InMemorySharesPort(Optional<SharesOutstandingSnapshot> initial) {
            initial.ifPresent(values::add);
        }

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

    private static class StaticFinancialPort implements QuarterlyFinancialPort {
        private final List<QuarterlyFinancial> values;

        StaticFinancialPort(List<QuarterlyFinancial> values) {
            this.values = values;
        }

        @Override public List<QuarterlyFinancial> saveAll(List<QuarterlyFinancial> values) { return values; }
        @Override public List<QuarterlyFinancial> findRecentQuarters(String stockCode, int limit) {
            return values.stream().limit(limit).toList();
        }
        @Override public Optional<QuarterlyFinancial> findByStockCodeAndQuarter(
                String stockCode, int fiscalYear, int fiscalQuarter
        ) { return Optional.empty(); }
    }

    private static class EmptyFinancialPort extends StaticFinancialPort {
        EmptyFinancialPort() {
            super(List.of());
        }
    }

    private static class StaticStockPort implements StockPort {
        @Override public Stock save(Stock stock) { return stock; }
        @Override public List<Stock> findAll() {
            return List.of(new Stock("005930", "Samsung", Market.KOSPI, true));
        }
    }

    private static class CountingAnalyzeUseCase implements AnalyzeEarningsUseCase {
        private int count;
        @Override public EarningsAnalysisSnapshot analyzeStock(String stockCode, LocalDate baseDate) {
            count++;
            return null;
        }
        @Override public List<EarningsAnalysisSnapshot> analyzeStocks(List<String> stockCodes, LocalDate baseDate) {
            return List.of();
        }
    }
}
