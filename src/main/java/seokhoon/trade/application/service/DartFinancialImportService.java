package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.AnalyzeEarningsUseCase;
import seokhoon.trade.application.port.in.ImportDartFinancialsUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.DartProperties;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.stock.Stock;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DartFinancialImportService implements ImportDartFinancialsUseCase {
    private final DartCorpMappingPort mappingPort;
    private final DartFinancialProviderPort providerPort;
    private final QuarterlyFinancialPort financialPort;
    private final DartFinancialImportHistoryPort historyPort;
    private final AnalyzeEarningsUseCase analyzeEarningsUseCase;
    private final StockPort stockPort;
    private final DartProperties properties;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public DartFinancialImportService(
            DartCorpMappingPort mappingPort,
            DartFinancialProviderPort providerPort,
            QuarterlyFinancialPort financialPort,
            DartFinancialImportHistoryPort historyPort,
            AnalyzeEarningsUseCase analyzeEarningsUseCase,
            StockPort stockPort,
            DartProperties properties,
            OperationalMetricsPort metrics
    ) {
        this(mappingPort, providerPort, financialPort, historyPort, analyzeEarningsUseCase,
                stockPort, properties, metrics, Clock.systemUTC());
    }

    DartFinancialImportService(
            DartCorpMappingPort mappingPort,
            DartFinancialProviderPort providerPort,
            QuarterlyFinancialPort financialPort,
            DartFinancialImportHistoryPort historyPort,
            AnalyzeEarningsUseCase analyzeEarningsUseCase,
            StockPort stockPort,
            DartProperties properties,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.mappingPort = mappingPort;
        this.providerPort = providerPort;
        this.financialPort = financialPort;
        this.historyPort = historyPort;
        this.analyzeEarningsUseCase = analyzeEarningsUseCase;
        this.stockPort = stockPort;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DartFinancialImportHistory importStock(String stockCode, int fiscalYear, String reportCode) {
        Instant requestedAt = clock.instant();
        Optional<DartCorpMapping> mapping = mappingPort.findByStockCode(stockCode);
        if (mapping.isEmpty()) {
            return saveHistory(stockCode, null, fiscalYear, reportCode,
                    DartFinancialImportStatus.SKIPPED, 0, "DART corp mapping not found", requestedAt);
        }
        try {
            DartFinancialStatement statement = providerPort.fetchFinancialStatement(
                    mapping.get().corpCode(), fiscalYear, reportCode);
            ImportMappingResult mapped = mapToQuarterly(stockCode, statement);
            int savedCount = 0;
            if (mapped.financial().isPresent()) {
                financialPort.saveAll(List.of(mapped.financial().orElseThrow()));
                savedCount = 1;
                if (properties.isImportAutoAnalyze()) {
                    analyzeEarningsUseCase.analyzeStock(stockCode, LocalDate.now(clock));
                }
            }
            DartFinancialImportStatus status = mapped.missingReasons().isEmpty()
                    ? DartFinancialImportStatus.SUCCESS
                    : DartFinancialImportStatus.PARTIAL;
            return saveHistory(stockCode, mapping.get().corpCode(), fiscalYear, reportCode,
                    status, savedCount, String.join(", ", mapped.missingReasons()), requestedAt);
        } catch (RuntimeException exception) {
            return saveHistory(stockCode, mapping.get().corpCode(), fiscalYear, reportCode,
                    DartFinancialImportStatus.FAILED, 0, sanitize(exception.getMessage()), requestedAt);
        }
    }

    @Override
    public List<DartFinancialImportHistory> importStockRecent(String stockCode, LocalDate baseDate) {
        return recentReportPeriods(baseDate).stream()
                .limit(properties.getImportLookbackQuarters())
                .map(period -> importStock(stockCode, period.fiscalYear(), period.reportCode()))
                .toList();
    }

    @Override
    public List<DartFinancialImportHistory> importActiveWatchlist(LocalDate baseDate) {
        return stockPort.findAll().stream()
                .filter(Stock::active)
                .flatMap(stock -> importStockRecent(stock.stockCode(), baseDate).stream())
                .toList();
    }

    private ImportMappingResult mapToQuarterly(String stockCode, DartFinancialStatement statement) {
        List<String> missing = new ArrayList<>();
        BigDecimal revenue = required(statement, missing, "revenue",
                DartFinancialAccountMapper.revenue(statement.accounts()));
        BigDecimal operatingIncome = required(statement, missing, "operating_income",
                DartFinancialAccountMapper.operatingIncome(statement.accounts()));
        BigDecimal netIncome = required(statement, missing, "net_income",
                DartFinancialAccountMapper.netIncome(statement.accounts()));
        BigDecimal totalAssets = required(statement, missing, "total_assets",
                DartFinancialAccountMapper.totalAssets(statement.accounts()));
        BigDecimal totalLiabilities = required(statement, missing, "total_liabilities",
                DartFinancialAccountMapper.totalLiabilities(statement.accounts()));
        BigDecimal totalEquity = required(statement, missing, "total_equity",
                DartFinancialAccountMapper.totalEquity(statement.accounts()));
        BigDecimal operatingCashFlow = required(statement, missing, "operating_cash_flow",
                DartFinancialAccountMapper.operatingCashFlow(statement.accounts()));
        if (!missing.isEmpty()) {
            return new ImportMappingResult(Optional.empty(), missing);
        }
        Instant now = clock.instant();
        QuarterlyFinancial financial = new QuarterlyFinancial(null, stockCode,
                statement.fiscalYear(), DartReportCode.fiscalQuarterOf(statement.reportCode()),
                revenue, operatingIncome, netIncome, totalAssets, totalLiabilities,
                totalEquity, operatingCashFlow, null, now, now);
        return new ImportMappingResult(Optional.of(financial), missing);
    }

    private BigDecimal required(
            DartFinancialStatement statement,
            List<String> missing,
            String field,
            Optional<BigDecimal> value
    ) {
        if (value.isEmpty()) {
            missing.add(field);
            return null;
        }
        return value.orElseThrow();
    }

    private DartFinancialImportHistory saveHistory(
            String stockCode,
            String corpCode,
            int fiscalYear,
            String reportCode,
            DartFinancialImportStatus status,
            int count,
            String reason,
            Instant requestedAt
    ) {
        DartFinancialImportHistory saved = historyPort.save(new DartFinancialImportHistory(
                null, stockCode, corpCode, fiscalYear, reportCode, status, count,
                reason == null || reason.isBlank() ? null : reason, requestedAt, clock.instant()));
        metrics.recordDartFinancialImport(metricResult(status));
        return saved;
    }

    private static List<ReportPeriod> recentReportPeriods(LocalDate baseDate) {
        List<ReportPeriod> periods = new ArrayList<>();
        int year = baseDate.getYear();
        int quarter = ((baseDate.getMonthValue() - 1) / 3) + 1;
        for (int i = 0; i < 16; i++) {
            periods.add(new ReportPeriod(year, DartReportCode.ofQuarter(quarter).code()));
            quarter--;
            if (quarter == 0) {
                quarter = 4;
                year--;
            }
        }
        return periods;
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "DART financial import failed";
        }
        String sanitized = message;
        String apiKey = properties.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            sanitized = sanitized.replace(apiKey, "[REDACTED]");
        }
        sanitized = sanitized.replaceAll("[\\r\\n\\t]", " ");
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }

    private static String metricResult(DartFinancialImportStatus status) {
        return status == DartFinancialImportStatus.FAILED ? "failure" : status.name().toLowerCase();
    }

    private record ImportMappingResult(Optional<QuarterlyFinancial> financial, List<String> missingReasons) {
    }

    private record ReportPeriod(int fiscalYear, String reportCode) {
    }
}
