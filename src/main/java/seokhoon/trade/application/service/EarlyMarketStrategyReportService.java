package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.EarlyMarketReportDataCompleteness;
import seokhoon.trade.application.port.in.EarlyMarketStrategyCandidateReport;
import seokhoon.trade.application.port.in.EarlyMarketStrategyDailyReport;
import seokhoon.trade.application.port.in.EarlyMarketStrategyGroupReport;
import seokhoon.trade.application.port.in.LoadEarlyMarketStrategyReportUseCase;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.EarlyMarketPerformancePort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.market.EarlyMarketCandidatePerformance;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EarlyMarketStrategyReportService
        implements LoadEarlyMarketStrategyReportUseCase {
    private static final Logger log =
            LoggerFactory.getLogger(EarlyMarketStrategyReportService.class);
    private static final String PREVIOUS_HIGH_BROKEN = "PREVIOUS_HIGH_BROKEN";
    private static final String PREVIOUS_HIGH_NOT_BROKEN = "PREVIOUS_HIGH_NOT_BROKEN";
    private static final String OPENING_PRICE_HELD = "OPENING_PRICE_HELD";
    private static final String OPENING_PRICE_LOST = "OPENING_PRICE_LOST";

    private final TradingSignalQueryPort tradingSignalQueryPort;
    private final EarlyMarketPerformancePort performancePort;
    private final OperationalMetricsPort metricsPort;

    public EarlyMarketStrategyReportService(
            TradingSignalQueryPort tradingSignalQueryPort,
            EarlyMarketPerformancePort performancePort,
            OperationalMetricsPort metricsPort
    ) {
        this.tradingSignalQueryPort = tradingSignalQueryPort;
        this.performancePort = performancePort;
        this.metricsPort = metricsPort;
    }

    @Override
    public EarlyMarketStrategyDailyReport loadDailyReport(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        try {
            List<TradingSignalRecord> signals = loadSignals(tradeDate);
            Map<Long, EarlyMarketCandidatePerformance> performanceBySignalId =
                    performancePort.findByTradeDate(tradeDate).stream()
                            .collect(Collectors.toMap(
                                    EarlyMarketCandidatePerformance::signalId,
                                    Function.identity(),
                                    (left, right) -> left
                            ));
            List<CandidateData> candidateData = signals.stream()
                    .filter(signal -> signal.id() != null)
                    .map(signal -> new CandidateData(
                            signal,
                            performanceBySignalId.get(signal.id())
                    ))
                    .toList();
            EarlyMarketStrategyDailyReport report = buildReport(tradeDate, candidateData);
            String metricResult = candidateData.isEmpty() ? "no_data" : "success";
            metricsPort.recordEarlyMarketReport(metricResult);
            logReport(report, metricResult);
            return report;
        } catch (RuntimeException exception) {
            metricsPort.recordEarlyMarketReport("failure");
            log.atError()
                    .addKeyValue("tradeDate", tradeDate)
                    .addKeyValue("result", "failure")
                    .setCause(exception)
                    .log("Early market strategy daily report failed");
            throw exception;
        }
    }

    private List<TradingSignalRecord> loadSignals(LocalDate tradeDate) {
        List<TradingSignalRecord> signals = new ArrayList<>();
        signals.addAll(loadSignals(tradeDate, SignalType.EARLY_MARKET_PRE_SCAN));
        signals.addAll(loadSignals(tradeDate, SignalType.EARLY_MARKET_ENTRY_CANDIDATE));
        return signals.stream()
                .sorted(Comparator.comparing(
                        TradingSignalRecord::id,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();
    }

    private List<TradingSignalRecord> loadSignals(
            LocalDate tradeDate,
            SignalType signalType
    ) {
        return tradingSignalQueryPort.find(new TradingSignalSearchCriteria(
                null,
                tradeDate,
                EarlyMarketPreOpenScanner.STRATEGY_NAME,
                signalType,
                null,
                null
        ));
    }

    private static EarlyMarketStrategyDailyReport buildReport(
            LocalDate tradeDate,
            List<CandidateData> candidates
    ) {
        List<EarlyMarketStrategyCandidateReport> items = candidates.stream()
                .map(EarlyMarketStrategyReportService::toItem)
                .toList();
        int capturedCount = (int) candidates.stream()
                .filter(candidate -> candidate.performance() != null)
                .count();
        int returnSampleCount = (int) candidates.stream()
                .filter(candidate -> maxReturn(candidate) != null)
                .count();
        int drawdownSampleCount = (int) candidates.stream()
                .filter(candidate -> maxDrawdown(candidate) != null)
                .count();
        EarlyMarketStrategyCandidateReport bestCandidate = candidates.stream()
                .filter(candidate -> maxReturn(candidate) != null)
                .max(Comparator.comparing(EarlyMarketStrategyReportService::maxReturn))
                .map(EarlyMarketStrategyReportService::toItem)
                .orElse(null);
        EarlyMarketStrategyCandidateReport worstCandidate = candidates.stream()
                .filter(candidate -> maxReturn(candidate) != null)
                .min(Comparator.comparing(EarlyMarketStrategyReportService::maxReturn))
                .map(EarlyMarketStrategyReportService::toItem)
                .orElse(null);

        return new EarlyMarketStrategyDailyReport(
                tradeDate,
                countSignalType(candidates, SignalType.EARLY_MARKET_PRE_SCAN),
                countSignalType(candidates, SignalType.EARLY_MARKET_ENTRY_CANDIDATE),
                capturedCount,
                candidates.size() - capturedCount,
                average(candidates, EarlyMarketStrategyReportService::maxReturn),
                average(candidates, EarlyMarketStrategyReportService::maxDrawdown),
                bestCandidate,
                worstCandidate,
                group(
                        candidates,
                        candidate -> candidate.signal().signalType().name(),
                        List.of(
                                SignalType.EARLY_MARKET_PRE_SCAN.name(),
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE.name()
                        )
                ),
                group(
                        candidates,
                        candidate -> scoreBucket(candidate.signal().score()),
                        List.of("70-79", "80-89", "90+")
                ),
                group(
                        candidates,
                        EarlyMarketStrategyReportService::vwapGroup,
                        List.of("TRUE", "FALSE", "UNKNOWN")
                ),
                group(
                        candidates,
                        EarlyMarketStrategyReportService::previousHighGroup,
                        List.of("TRUE", "FALSE", "UNKNOWN")
                ),
                group(
                        candidates,
                        EarlyMarketStrategyReportService::openingPriceGroup,
                        List.of("TRUE", "FALSE", "UNKNOWN")
                ),
                new EarlyMarketReportDataCompleteness(
                        candidates.size(),
                        capturedCount,
                        candidates.size() - capturedCount,
                        returnSampleCount,
                        drawdownSampleCount
                ),
                items
        );
    }

    private static Map<String, EarlyMarketStrategyGroupReport> group(
            List<CandidateData> candidates,
            Function<CandidateData, String> classifier,
            List<String> defaultKeys
    ) {
        Map<String, List<CandidateData>> grouped = candidates.stream()
                .collect(Collectors.groupingBy(
                        classifier,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<String, EarlyMarketStrategyGroupReport> result = new LinkedHashMap<>();
        defaultKeys.forEach(key -> result.put(
                key,
                new EarlyMarketStrategyGroupReport(0, 0, null, null)
        ));
        grouped.forEach((key, values) -> result.put(key, new EarlyMarketStrategyGroupReport(
                values.size(),
                (int) values.stream()
                        .filter(candidate -> candidate.performance() != null)
                        .count(),
                average(values, EarlyMarketStrategyReportService::maxReturn),
                average(values, EarlyMarketStrategyReportService::maxDrawdown)
        )));
        return Map.copyOf(result);
    }

    private static BigDecimal average(
            List<CandidateData> candidates,
            Function<CandidateData, BigDecimal> valueExtractor
    ) {
        List<BigDecimal> values = candidates.stream()
                .map(valueExtractor)
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private static int countSignalType(
            List<CandidateData> candidates,
            SignalType signalType
    ) {
        return (int) candidates.stream()
                .filter(candidate -> candidate.signal().signalType() == signalType)
                .count();
    }

    private static String scoreBucket(int score) {
        if (score >= 90) {
            return "90+";
        }
        if (score >= 80) {
            return "80-89";
        }
        if (score >= 70) {
            return "70-79";
        }
        return "BELOW_70";
    }

    private static String vwapGroup(CandidateData candidate) {
        if (candidate.performance() == null
                || candidate.performance().vwapBroken() == null) {
            return "UNKNOWN";
        }
        return candidate.performance().vwapBroken() ? "TRUE" : "FALSE";
    }

    private static String previousHighGroup(CandidateData candidate) {
        if (candidate.signal().reasons().contains(PREVIOUS_HIGH_BROKEN)) {
            return "TRUE";
        }
        if (candidate.signal().reasons().contains(PREVIOUS_HIGH_NOT_BROKEN)) {
            return "FALSE";
        }
        return "UNKNOWN";
    }

    private static String openingPriceGroup(CandidateData candidate) {
        if (candidate.signal().reasons().contains(OPENING_PRICE_HELD)) {
            return "TRUE";
        }
        if (candidate.signal().reasons().contains(OPENING_PRICE_LOST)) {
            return "FALSE";
        }
        return "UNKNOWN";
    }

    private static BigDecimal maxReturn(CandidateData candidate) {
        return candidate.performance() == null
                ? null
                : candidate.performance().maxReturnRateUntil0930();
    }

    private static BigDecimal maxDrawdown(CandidateData candidate) {
        return candidate.performance() == null
                ? null
                : candidate.performance().maxDrawdownRateUntil0930();
    }

    private static EarlyMarketStrategyCandidateReport toItem(CandidateData candidate) {
        EarlyMarketCandidatePerformance performance = candidate.performance();
        return new EarlyMarketStrategyCandidateReport(
                candidate.signal().id(),
                candidate.signal().stockCode(),
                candidate.signal().signalType(),
                candidate.signal().score(),
                performance == null ? null : performance.maxReturnRateUntil0930(),
                performance == null ? null : performance.maxDrawdownRateUntil0930(),
                performance == null ? null : performance.vwapBroken(),
                List.copyOf(candidate.signal().reasons()),
                List.copyOf(candidate.signal().riskReasons())
        );
    }

    private static void logReport(
            EarlyMarketStrategyDailyReport report,
            String result
    ) {
        log.atInfo()
                .addKeyValue("tradeDate", report.tradeDate())
                .addKeyValue("preScanCount", report.preScanCount())
                .addKeyValue("entryCandidateCount", report.entryCandidateCount())
                .addKeyValue(
                        "performanceCapturedCount",
                        report.performanceCapturedCount()
                )
                .addKeyValue(
                        "excludedFromPerformanceCount",
                        report.excludedFromPerformanceCount()
                )
                .addKeyValue("result", result)
                .log("Early market strategy daily report loaded");
    }

    private record CandidateData(
            TradingSignalRecord signal,
            EarlyMarketCandidatePerformance performance
    ) {
    }
}
