package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.AnalyzeEarningsUseCase;
import seokhoon.trade.application.port.in.GenerateValuationSnapshotUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.ResearchProperties;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.stock.Stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ValuationSnapshotGenerationService implements GenerateValuationSnapshotUseCase {
    private static final int SCALE = 4;

    private final DailyPricePort dailyPricePort;
    private final QuarterlyFinancialPort financialPort;
    private final SharesOutstandingSnapshotPort sharesOutstandingPort;
    private final ValuationSnapshotPort valuationPort;
    private final StockPort stockPort;
    private final AnalyzeEarningsUseCase analyzeEarningsUseCase;
    private final ResearchProperties properties;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public ValuationSnapshotGenerationService(
            DailyPricePort dailyPricePort,
            QuarterlyFinancialPort financialPort,
            SharesOutstandingSnapshotPort sharesOutstandingPort,
            ValuationSnapshotPort valuationPort,
            StockPort stockPort,
            AnalyzeEarningsUseCase analyzeEarningsUseCase,
            ResearchProperties properties,
            OperationalMetricsPort metrics
    ) {
        this(dailyPricePort, financialPort, sharesOutstandingPort, valuationPort,
                stockPort, analyzeEarningsUseCase, properties, metrics, Clock.systemUTC());
    }

    ValuationSnapshotGenerationService(
            DailyPricePort dailyPricePort,
            QuarterlyFinancialPort financialPort,
            SharesOutstandingSnapshotPort sharesOutstandingPort,
            ValuationSnapshotPort valuationPort,
            StockPort stockPort,
            AnalyzeEarningsUseCase analyzeEarningsUseCase,
            ResearchProperties properties,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.dailyPricePort = dailyPricePort;
        this.financialPort = financialPort;
        this.sharesOutstandingPort = sharesOutstandingPort;
        this.valuationPort = valuationPort;
        this.stockPort = stockPort;
        this.analyzeEarningsUseCase = analyzeEarningsUseCase;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ValuationGenerationResult generate(String stockCode, LocalDate baseDate) {
        Objects.requireNonNull(stockCode, "stockCode");
        Objects.requireNonNull(baseDate, "baseDate");
        if (!properties.isValuationAutoSnapshotEnabled()) {
            metrics.recordResearchValuationAutoSnapshot("insufficient");
            return insufficient(stockCode, baseDate, "VALUATION_AUTO_DISABLED");
        }
        try {
            ValuationGenerationResult result = generateInternal(stockCode, baseDate);
            metrics.recordResearchValuationAutoSnapshot(
                    result.status() == ValuationGenerationStatus.GENERATED ? "success" : "insufficient");
            return result;
        } catch (RuntimeException exception) {
            metrics.recordResearchValuationAutoSnapshot("failure");
            return new ValuationGenerationResult(stockCode, baseDate,
                    ValuationGenerationStatus.FAILED, null, List.of(failureReason(exception)));
        }
    }

    @Override
    @Transactional
    public List<ValuationGenerationResult> generateBatch(List<String> stockCodes, LocalDate baseDate) {
        return stockCodes.stream()
                .map(stockCode -> generate(stockCode, baseDate))
                .toList();
    }

    @Override
    @Transactional
    public List<ValuationGenerationResult> generateWatchlist(LocalDate baseDate) {
        List<String> stockCodes = stockPort.findAll().stream()
                .filter(Stock::active)
                .map(Stock::stockCode)
                .toList();
        return generateBatch(stockCodes, baseDate);
    }

    private ValuationGenerationResult generateInternal(String stockCode, LocalDate baseDate) {
        Optional<DailyPrice> price = latestPrice(stockCode, baseDate);
        if (price.isEmpty()) {
            return insufficient(stockCode, baseDate,
                    "VALUATION_DATA_INSUFFICIENT daily price 없음");
        }
        Optional<QuarterlyFinancial> financial = financialPort.findRecentQuarters(stockCode, 1).stream()
                .findFirst();
        if (financial.isEmpty()) {
            return insufficient(stockCode, baseDate,
                    "VALUATION_DATA_INSUFFICIENT quarterly financial 없음");
        }
        Optional<SharesOutstandingSnapshot> shares = sharesOutstandingPort
                .findLatestSharesByStockCode(stockCode, baseDate);
        if (shares.isEmpty()) {
            return insufficient(stockCode, baseDate,
                    "SHARES_OUTSTANDING_REQUIRED latest shares outstanding 없음");
        }
        return saveSnapshot(stockCode, baseDate, price.orElseThrow(), financial.orElseThrow(), shares.orElseThrow());
    }

    private ValuationGenerationResult saveSnapshot(
            String stockCode,
            LocalDate baseDate,
            DailyPrice price,
            QuarterlyFinancial financial,
            SharesOutstandingSnapshot shares
    ) {
        BigDecimal sharesOutstanding = shares.sharesOutstanding();
        BigDecimal marketCap = price.closePrice().multiply(sharesOutstanding).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal eps = divide(financial.netIncome(), sharesOutstanding);
        BigDecimal bps = divide(financial.totalEquity(), sharesOutstanding);
        BigDecimal salesPerShare = divide(financial.revenue(), sharesOutstanding);
        List<String> reasons = new ArrayList<>();
        BigDecimal per = null;
        BigDecimal pbr = null;
        BigDecimal psr = null;
        if (financial.netIncome().signum() > 0) {
            per = divide(marketCap, financial.netIncome());
        } else {
            reasons.add("NEGATIVE_EARNINGS");
        }
        if (financial.totalEquity().signum() > 0) {
            pbr = divide(marketCap, financial.totalEquity());
        } else {
            reasons.add("NEGATIVE_EQUITY");
        }
        if (financial.revenue().signum() > 0) {
            psr = divide(marketCap, financial.revenue());
        } else {
            reasons.add("INVALID_REVENUE");
        }
        reasons.add("VALUATION_AUTO_GENERATED");
        Instant now = clock.instant();
        ValuationSnapshot saved = valuationPort.save(new ValuationSnapshot(
                null, stockCode, baseDate, marketCap, per, pbr, psr,
                eps, bps, salesPerShare, ValuationSnapshotSource.AUTO, now, now));
        if (properties.isValuationAutoSnapshotAutoAnalyze()) {
            analyzeEarningsUseCase.analyzeStock(stockCode, baseDate);
        }
        return new ValuationGenerationResult(stockCode, baseDate,
                ValuationGenerationStatus.GENERATED, saved, reasons);
    }

    private Optional<DailyPrice> latestPrice(String stockCode, LocalDate baseDate) {
        LocalDate from = baseDate.minusDays(properties.getValuationAutoSnapshotLookbackDays());
        return dailyPricePort.findByStockCodeAndTradeDateBetween(stockCode, from, baseDate)
                .stream()
                .max(Comparator.comparing(DailyPrice::tradeDate));
    }

    private static ValuationGenerationResult insufficient(String stockCode, LocalDate baseDate, String reason) {
        return new ValuationGenerationResult(stockCode, baseDate,
                ValuationGenerationStatus.DATA_INSUFFICIENT, null, List.of(reason));
    }

    private static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return null;
        }
        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private static String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        String reason = exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
    }
}
