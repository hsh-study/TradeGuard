package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.in.ResearchUseCases.MorningNoteUseCase;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.market.MarketIndex;
import seokhoon.trade.domain.market.Sector;
import seokhoon.trade.domain.market.SectorDailySnapshot;
import seokhoon.trade.domain.position.LivePosition;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.stock.Stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MorningNoteService implements MorningNoteUseCase {
    private static final int LOOKBACK_DAYS = 180;
    private static final int UPCOMING_DAYS = 14;

    private final MorningNotePort notePort;
    private final StockPort stockPort;
    private final DailyPricePort dailyPricePort;
    private final IndicatorSnapshotPort indicatorPort;
    private final TradingSignalQueryPort signalPort;
    private final LivePositionPort positionPort;
    private final InvestmentThesisPort thesisPort;
    private final InvestmentCatalystPort catalystPort;
    private final MarketCalendarPort calendarPort;
    private final MarketIndexPort marketIndexPort;
    private final SectorPort sectorPort;
    private final StockSectorMappingPort stockSectorMappingPort;
    private final SectorDailySnapshotPort sectorSnapshotPort;
    private final EarningsAnalysisPort earningsAnalysisPort;
    private final EarningsEventPort earningsEventPort;
    private final EarningsPreviewPort earningsPreviewPort;
    private final PostEarningsReviewPort postEarningsReviewPort;
    private final DartCorpMappingPort dartCorpMappingPort;
    private final DartFinancialImportHistoryPort dartFinancialImportHistoryPort;
    private final ValuationSnapshotPort valuationSnapshotPort;
    private final SharesOutstandingSnapshotPort sharesOutstandingSnapshotPort;
    private final DartCorpCodeImportHistoryPort dartCorpCodeImportHistoryPort;
    private final SharesOutstandingImportHistoryPort sharesOutstandingImportHistoryPort;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public MorningNoteService(
            MorningNotePort notePort,
            StockPort stockPort,
            DailyPricePort dailyPricePort,
            IndicatorSnapshotPort indicatorPort,
            TradingSignalQueryPort signalPort,
            LivePositionPort positionPort,
            InvestmentThesisPort thesisPort,
            InvestmentCatalystPort catalystPort,
            MarketCalendarPort calendarPort,
            MarketIndexPort marketIndexPort,
            SectorPort sectorPort,
            StockSectorMappingPort stockSectorMappingPort,
            SectorDailySnapshotPort sectorSnapshotPort,
            EarningsAnalysisPort earningsAnalysisPort,
            EarningsEventPort earningsEventPort,
            EarningsPreviewPort earningsPreviewPort,
            PostEarningsReviewPort postEarningsReviewPort,
            DartCorpMappingPort dartCorpMappingPort,
            DartFinancialImportHistoryPort dartFinancialImportHistoryPort,
            ValuationSnapshotPort valuationSnapshotPort,
            SharesOutstandingSnapshotPort sharesOutstandingSnapshotPort,
            DartCorpCodeImportHistoryPort dartCorpCodeImportHistoryPort,
            SharesOutstandingImportHistoryPort sharesOutstandingImportHistoryPort,
            OperationalMetricsPort metrics
    ) {
        this(notePort, stockPort, dailyPricePort, indicatorPort, signalPort, positionPort,
                thesisPort, catalystPort, calendarPort, marketIndexPort, sectorPort,
                stockSectorMappingPort, sectorSnapshotPort, earningsAnalysisPort,
                earningsEventPort, earningsPreviewPort, postEarningsReviewPort,
                dartCorpMappingPort, dartFinancialImportHistoryPort,
                valuationSnapshotPort, sharesOutstandingSnapshotPort,
                dartCorpCodeImportHistoryPort, sharesOutstandingImportHistoryPort,
                metrics, Clock.systemUTC());
    }

    MorningNoteService(
            MorningNotePort notePort,
            StockPort stockPort,
            DailyPricePort dailyPricePort,
            IndicatorSnapshotPort indicatorPort,
            TradingSignalQueryPort signalPort,
            LivePositionPort positionPort,
            InvestmentThesisPort thesisPort,
            InvestmentCatalystPort catalystPort,
            MarketCalendarPort calendarPort,
            MarketIndexPort marketIndexPort,
            SectorPort sectorPort,
            StockSectorMappingPort stockSectorMappingPort,
            SectorDailySnapshotPort sectorSnapshotPort,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this(notePort, stockPort, dailyPricePort, indicatorPort, signalPort, positionPort,
                thesisPort, catalystPort, calendarPort, marketIndexPort, sectorPort,
                stockSectorMappingPort, sectorSnapshotPort, null, null, null, null,
                null, null, null, null, null, null, metrics, clock);
    }

    MorningNoteService(
            MorningNotePort notePort,
            StockPort stockPort,
            DailyPricePort dailyPricePort,
            IndicatorSnapshotPort indicatorPort,
            TradingSignalQueryPort signalPort,
            LivePositionPort positionPort,
            InvestmentThesisPort thesisPort,
            InvestmentCatalystPort catalystPort,
            MarketCalendarPort calendarPort,
            MarketIndexPort marketIndexPort,
            SectorPort sectorPort,
            StockSectorMappingPort stockSectorMappingPort,
            SectorDailySnapshotPort sectorSnapshotPort,
            EarningsAnalysisPort earningsAnalysisPort,
            EarningsEventPort earningsEventPort,
            EarningsPreviewPort earningsPreviewPort,
            PostEarningsReviewPort postEarningsReviewPort,
            DartCorpMappingPort dartCorpMappingPort,
            DartFinancialImportHistoryPort dartFinancialImportHistoryPort,
            ValuationSnapshotPort valuationSnapshotPort,
            SharesOutstandingSnapshotPort sharesOutstandingSnapshotPort,
            DartCorpCodeImportHistoryPort dartCorpCodeImportHistoryPort,
            SharesOutstandingImportHistoryPort sharesOutstandingImportHistoryPort,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.notePort = notePort;
        this.stockPort = stockPort;
        this.dailyPricePort = dailyPricePort;
        this.indicatorPort = indicatorPort;
        this.signalPort = signalPort;
        this.positionPort = positionPort;
        this.thesisPort = thesisPort;
        this.catalystPort = catalystPort;
        this.calendarPort = calendarPort;
        this.marketIndexPort = marketIndexPort;
        this.sectorPort = sectorPort;
        this.stockSectorMappingPort = stockSectorMappingPort;
        this.sectorSnapshotPort = sectorSnapshotPort;
        this.earningsAnalysisPort = earningsAnalysisPort;
        this.earningsEventPort = earningsEventPort;
        this.earningsPreviewPort = earningsPreviewPort;
        this.postEarningsReviewPort = postEarningsReviewPort;
        this.dartCorpMappingPort = dartCorpMappingPort;
        this.dartFinancialImportHistoryPort = dartFinancialImportHistoryPort;
        this.valuationSnapshotPort = valuationSnapshotPort;
        this.sharesOutstandingSnapshotPort = sharesOutstandingSnapshotPort;
        this.dartCorpCodeImportHistoryPort = dartCorpCodeImportHistoryPort;
        this.sharesOutstandingImportHistoryPort = sharesOutstandingImportHistoryPort;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    public MorningNote generate(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        try {
            LocalDate previousTradingDay = calendarPort.previousTradingDay(tradeDate);
            List<Stock> stocks = stockPort.findAll().stream().filter(Stock::active).toList();
            List<LivePosition> positions = positionPort.findOpenPositions();
            List<TradingSignalRecord> signals = signalPort.find(new TradingSignalSearchCriteria(
                    null, previousTradingDay, null, null, null, null));
            List<InvestmentCatalyst> catalysts = catalystPort.find(
                    null, tradeDate, tradeDate.plusDays(UPCOMING_DAYS), CatalystStatus.UPCOMING);
            List<InvestmentThesis> brokenTheses = thesisPort.find(null, ThesisStatus.BROKEN);

            MorningNote saved = notePort.save(new MorningNote(
                    null,
                    tradeDate,
                    marketSummary(previousTradingDay, signals, marketIndexPort.findByTradeDate(previousTradingDay)),
                    sectorSummary(previousTradingDay),
                    portfolioSummary(tradeDate, positions),
                    watchlistSummary(tradeDate, stocks),
                    actionItems(catalysts, brokenTheses, stocks, signals, tradeDate),
                    clock.instant()
            ));
            boolean noData = stocks.isEmpty() && positions.isEmpty() && signals.isEmpty()
                    && catalysts.isEmpty() && brokenTheses.isEmpty();
            metrics.recordResearchMorningNote(noData ? "no_data" : "success");
            return saved;
        } catch (RuntimeException exception) {
            metrics.recordResearchMorningNote("failure");
            throw exception;
        }
    }

    @Override
    public MorningNote load(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        return notePort.findByTradeDate(tradeDate)
                .orElseThrow(() -> new ResearchNotFoundException("Morning note not found: " + tradeDate));
    }

    private static String marketSummary(
            LocalDate previousTradingDay,
            List<TradingSignalRecord> signals,
            List<MarketIndex> indices
    ) {
        StringBuilder result = new StringBuilder()
                .append("전 거래일 ").append(previousTradingDay)
                .append(" 저장 후보 ").append(signals.size()).append("건");
        if (indices.isEmpty()) {
            result.append("\n시장 지수 당일 변화는 데이터 소스 미연결로 제공하지 않습니다.");
        } else {
            result.append("\n시장 지수");
            indices.forEach(index -> result.append("\n- ")
                    .append(index.indexName()).append("(").append(index.indexCode()).append(")")
                    .append(" close=").append(index.closePrice())
                    .append(" changeRate=").append(index.changeRate()).append("%")
                    .append(" tradingValue=").append(index.tradingValue()));
        }
        signals.stream()
                .sorted(Comparator.comparingInt(TradingSignalRecord::score).reversed())
                .limit(5)
                .forEach(signal -> result.append("\n- ")
                        .append(signal.strategyName()).append(" ")
                        .append(signal.stockCode()).append(" score=")
                        .append(signal.score()).append(" status=").append(signal.status()));
        return result.toString();
    }

    private String sectorSummary(LocalDate previousTradingDay) {
        List<SectorDailySnapshot> snapshots = sectorSnapshotPort.findByTradeDate(previousTradingDay);
        if (snapshots.isEmpty()) {
            return "SECTOR_DATA_UNAVAILABLE: 섹터 스냅샷 데이터가 아직 생성되지 않았습니다.";
        }
        Map<String, Sector> sectors = sectorPort.findAll().stream()
                .collect(Collectors.toMap(Sector::sectorCode, Function.identity(), (left, right) -> left));
        List<SectorDailySnapshot> valid = snapshots.stream()
                .filter(snapshot -> !snapshot.dataInsufficient())
                .toList();
        if (valid.isEmpty()) {
            return "SECTOR_DATA_UNAVAILABLE: 섹터 구성 종목의 일봉 데이터가 부족합니다.";
        }
        StringBuilder result = new StringBuilder("섹터 가격/거래대금 요약 기준일 ").append(previousTradingDay);
        appendSectorBlock(result, "상위 섹터", valid.stream()
                .sorted(Comparator.comparing(SectorDailySnapshot::averageChangeRate).reversed())
                .limit(5).toList(), sectors);
        appendSectorBlock(result, "하위 섹터", valid.stream()
                .sorted(Comparator.comparing(SectorDailySnapshot::averageChangeRate))
                .limit(5).toList(), sectors);
        snapshots.stream()
                .filter(SectorDailySnapshot::dataInsufficient)
                .forEach(snapshot -> result.append("\n- DATA_INSUFFICIENT ")
                        .append(sectorLabel(snapshot.sectorCode(), sectors)));
        return result.toString();
    }

    private static void appendSectorBlock(
            StringBuilder result,
            String title,
            List<SectorDailySnapshot> snapshots,
            Map<String, Sector> sectors
    ) {
        result.append("\n").append(title);
        snapshots.forEach(snapshot -> result.append("\n- ")
                .append(sectorLabel(snapshot.sectorCode(), sectors))
                .append(" avg=").append(snapshot.averageChangeRate()).append("%")
                .append(" median=").append(snapshot.medianChangeRate()).append("%")
                .append(" value=").append(snapshot.totalTradingValue())
                .append(" up/down=").append(snapshot.risingStockCount())
                .append("/").append(snapshot.fallingStockCount())
                .append(" leader=").append(snapshot.leadingStockCode())
                .append("(").append(snapshot.leadingStockChangeRate()).append("%)"));
    }

    private static String sectorLabel(String sectorCode, Map<String, Sector> sectors) {
        Sector sector = sectors.get(sectorCode);
        return sector == null ? sectorCode : sector.sectorName() + "(" + sector.sectorCode() + ")";
    }

    private String portfolioSummary(LocalDate tradeDate, List<LivePosition> positions) {
        if (positions.isEmpty()) {
            return "열린 보유 포지션 없음";
        }
        StringBuilder result = new StringBuilder("열린 보유 포지션 ").append(positions.size()).append("건");
        positions.forEach(position -> {
            Optional<DailyPrice> price = latestPrice(position.stockCode(), tradeDate);
            result.append("\n- ").append(position.stockCode())
                    .append(" qty=").append(position.quantity())
                    .append(" avg=").append(position.averageBuyPrice())
                    .append(" earnings=").append(earningsStatus(position.stockCode()));
            if (price.isPresent() && position.averageBuyPrice().signum() > 0) {
                BigDecimal change = price.get().closePrice()
                        .subtract(position.averageBuyPrice())
                        .divide(position.averageBuyPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                result.append(" close=").append(price.get().closePrice())
                        .append(" unrealizedRate=").append(change).append("%");
            } else {
                result.append(" DATA_INSUFFICIENT");
            }
        });
        return result.toString();
    }

    private String watchlistSummary(LocalDate tradeDate, List<Stock> stocks) {
        if (stocks.isEmpty()) {
            return "활성 관심종목 없음";
        }
        StringBuilder result = new StringBuilder("활성 관심종목 ").append(stocks.size()).append("개");
        Map<String, Sector> sectors = sectorPort.findAll().stream()
                .collect(Collectors.toMap(Sector::sectorCode, Function.identity(), (left, right) -> left));
        stocks.forEach(stock -> result.append("\n- ")
                .append(indicatorLine(stock, tradeDate))
                .append(" sectors=").append(stockSectorLabels(stock.stockCode(), sectors)));
        return result.toString();
    }

    private String stockSectorLabels(String stockCode, Map<String, Sector> sectors) {
        List<String> labels = stockSectorMappingPort.findByStockCode(stockCode).stream()
                .map(mapping -> sectorLabel(mapping.sectorCode(), sectors))
                .toList();
        return labels.isEmpty() ? "UNMAPPED" : String.join(",", labels);
    }

    private String indicatorLine(Stock stock, LocalDate tradeDate) {
        Optional<DailyPrice> price = latestPrice(stock.stockCode(), tradeDate);
        Optional<IndicatorSnapshot> indicator = latestIndicator(stock.stockCode(), tradeDate);
        if (price.isEmpty() || indicator.isEmpty() || incomplete(indicator.get())) {
            return stock.stockCode() + " " + stock.stockName() + " DATA_INSUFFICIENT";
        }
        BigDecimal close = price.get().closePrice();
        IndicatorSnapshot value = indicator.get();
        return stock.stockCode() + " " + stock.stockName()
                + " close=" + close
                + " vsMA20=" + side(close, value.ma20())
                + " vsMA60=" + side(close, value.ma60())
                + " ma20>ma60=" + (value.ma20().compareTo(value.ma60()) > 0)
                + " RSI=" + rsiState(value.rsi14())
                + " MACD=" + macdState(value)
                + " Bollinger=" + bollingerState(close, value)
                + " earnings=" + earningsStatus(stock.stockCode());
    }

    private String actionItems(
            List<InvestmentCatalyst> catalysts,
            List<InvestmentThesis> brokenTheses,
            List<Stock> stocks,
            List<TradingSignalRecord> signals,
            LocalDate tradeDate
    ) {
        StringBuilder result = new StringBuilder("자동 주문 없음. 수동 리서치 체크리스트");
        catalysts.forEach(catalyst -> result.append("\n- UPCOMING_CATALYST ")
                .append(catalyst.expectedDate()).append(" ")
                .append(catalyst.stockCode() == null ? "MARKET" : catalyst.stockCode())
                .append(" [").append(catalyst.importance()).append("] ")
                .append(catalyst.title()));
        brokenTheses.forEach(thesis -> result.append("\n- BROKEN_THESIS ")
                .append(thesis.stockCode()).append(" ")
                .append(thesis.title()).append(": ")
                .append(thesis.invalidationCondition()));
        stocks.stream()
                .filter(stock -> latestPrice(stock.stockCode(), tradeDate).isEmpty()
                        || latestIndicator(stock.stockCode(), tradeDate).filter(i -> !incomplete(i)).isEmpty())
                .forEach(stock -> result.append("\n- DATA_INSUFFICIENT ")
                        .append(stock.stockCode()).append(" 일봉/지표 보강 확인"));
        stocks.forEach(stock -> latestEarnings(stock.stockCode()).ifPresentOrElse(earnings -> {
            switch (earnings.status()) {
                case DATA_INSUFFICIENT -> result.append("\n- EARNINGS_DATA_INSUFFICIENT ")
                        .append(stock.stockCode()).append(" 최근 4분기 미만");
                case STRONG -> {
                    if (technicallyFavorable(stock.stockCode(), tradeDate)) {
                        result.append("\n- EARNINGS_STRONG ")
                                .append(stock.stockCode())
                                .append(" overallScore=").append(earnings.overallScore())
                                .append(" revenueYoY=").append(earnings.revenueYoyGrowth())
                                .append(" operatingMargin=").append(earnings.operatingMargin());
                    }
                }
                case WEAK -> {
                }
                case NEUTRAL -> {
                }
            }
        }, () -> result.append("\n- EARNINGS_DATA_INSUFFICIENT ")
                .append(stock.stockCode()).append(" earnings analysis 없음")));
        signals.stream()
                .filter(signal -> latestEarnings(signal.stockCode())
                        .filter(earnings -> earnings.status() == EarningsAnalysisStatus.WEAK)
                        .isPresent())
                .forEach(signal -> result.append("\n- EARNINGS_WEAK_BUT_SIGNAL ")
                        .append(signal.stockCode()).append(" ")
                        .append(signal.strategyName()).append(" 후보지만 실적 품질 약함"));
        upcomingEarningsEvents(tradeDate, tradeDate.plusDays(7))
                .forEach(event -> result.append("\n- UPCOMING_EARNINGS ")
                        .append(event.expectedAnnouncementDate()).append(" ")
                        .append(event.stockCode()).append(" ")
                        .append(event.fiscalYear()).append("Q").append(event.fiscalQuarter()));
        readyPreviews(tradeDate, tradeDate.plusDays(7))
                .forEach(preview -> result.append("\n- EARNINGS_PREVIEW_READY ")
                        .append(preview.stockCode())
                        .append(" previewDate=").append(preview.previewDate())
                        .append(" checkpoints=").append(preview.keyCheckpoints()));
        announcedNotReviewed(tradeDate.minusDays(30), tradeDate)
                .forEach(event -> result.append("\n- EARNINGS_REVIEW_REQUIRED ")
                        .append(event.stockCode()).append(" ")
                        .append(event.fiscalYear()).append("Q").append(event.fiscalQuarter())
                        .append(" announced=").append(event.actualAnnouncementDate()));
        negativeReviews()
                .forEach(review -> result.append("\n- POST_EARNINGS_")
                        .append(review.thesisImpact()).append(" ")
                        .append(review.stockCode())
                        .append(" revenueSurprise=").append(review.revenueSurpriseRate())
                        .append(" opSurprise=").append(review.operatingIncomeSurpriseRate()));
        appendDartItems(result, stocks);
        appendValuationItems(result, stocks, tradeDate);
        appendImportHistoryItems(result);
        if (catalysts.isEmpty() && brokenTheses.isEmpty() && stocks.isEmpty()) {
            result.append("\n- 등록된 리서치 대상 없음");
        }
        return result.toString();
    }

    private void appendValuationItems(StringBuilder result, List<Stock> stocks, LocalDate tradeDate) {
        if (valuationSnapshotPort == null || sharesOutstandingSnapshotPort == null) {
            return;
        }
        stocks.forEach(stock -> {
            boolean sharesMissing = sharesOutstandingSnapshotPort
                    .findLatestSharesByStockCode(stock.stockCode(), tradeDate)
                    .isEmpty();
            if (sharesMissing) {
                result.append("\n- SHARES_OUTSTANDING_REQUIRED ")
                        .append(stock.stockCode()).append(" 발행주식수 snapshot 필요");
                result.append("\n- SHARES_OUTSTANDING_IMPORT_REQUIRED ")
                        .append(stock.stockCode()).append(" CSV/provider import 필요");
            }
            Optional<ValuationSnapshot> valuation = valuationSnapshotPort
                    .findLatestByStockCode(stock.stockCode(), tradeDate);
            if (valuation.isEmpty()) {
                result.append("\n- VALUATION_DATA_INSUFFICIENT ")
                        .append(stock.stockCode()).append(" valuation snapshot 없음");
                return;
            }
            ValuationSnapshot snapshot = valuation.orElseThrow();
            if (snapshot.source() == ValuationSnapshotSource.AUTO) {
                result.append("\n- VALUATION_AUTO_GENERATED ")
                        .append(stock.stockCode()).append(" tradeDate=")
                        .append(snapshot.tradeDate());
            }
            if (snapshot.per() == null) {
                result.append("\n- VALUATION_NEGATIVE_EARNINGS ")
                        .append(stock.stockCode()).append(" PER 계산 제외");
            }
            if (overvalued(snapshot)) {
                result.append("\n- VALUATION_OVERVALUED_WARNING ")
                        .append(stock.stockCode())
                        .append(" per=").append(snapshot.per())
                        .append(" pbr=").append(snapshot.pbr())
                        .append(" psr=").append(snapshot.psr());
            }
        });
    }

    private void appendImportHistoryItems(StringBuilder result) {
        if (dartCorpCodeImportHistoryPort != null) {
            dartCorpCodeImportHistoryPort.findAllCorpCodeImports().stream().findFirst()
                    .ifPresent(history -> {
                        if (history.status() == DartCorpCodeImportStatus.SUCCESS
                                || history.status() == DartCorpCodeImportStatus.PARTIAL) {
                            result.append("\n- DART_CORP_MAPPING_IMPORTED matched=")
                                    .append(history.matchedStockCount())
                                    .append(" imported=").append(history.importedCount());
                        }
                        if (history.status() == DartCorpCodeImportStatus.FAILED) {
                            result.append("\n- DART_CORP_MAPPING_IMPORT_FAILED ")
                                    .append(history.failureReason());
                        }
                    });
        }
        if (sharesOutstandingImportHistoryPort != null) {
            sharesOutstandingImportHistoryPort.findAllSharesOutstandingImports().stream().findFirst()
                    .ifPresent(history -> {
                        if (history.status() == SharesOutstandingImportStatus.SUCCESS
                                || history.status() == SharesOutstandingImportStatus.PARTIAL) {
                            result.append("\n- SHARES_OUTSTANDING_IMPORTED imported=")
                                    .append(history.importedCount());
                        }
                        if (history.status() == SharesOutstandingImportStatus.FAILED) {
                            result.append("\n- SHARES_OUTSTANDING_IMPORT_FAILED ")
                                    .append(history.failureReason());
                        }
                    });
        }
    }

    private static boolean overvalued(ValuationSnapshot snapshot) {
        return greaterThan(snapshot.per(), "30")
                || greaterThan(snapshot.pbr(), "3")
                || greaterThan(snapshot.psr(), "5");
    }

    private void appendDartItems(StringBuilder result, List<Stock> stocks) {
        if (dartCorpMappingPort == null) {
            return;
        }
        stocks.stream()
                .filter(stock -> dartCorpMappingPort.findByStockCode(stock.stockCode()).isEmpty())
                .forEach(stock -> result.append("\n- DART_MAPPING_REQUIRED ")
                        .append(stock.stockCode()).append(" corp_code mapping 필요"));
        stocks.stream()
                .filter(stock -> latestEarnings(stock.stockCode())
                        .filter(earnings -> earnings.status() == EarningsAnalysisStatus.DATA_INSUFFICIENT)
                        .isPresent())
                .forEach(stock -> result.append("\n- DART_IMPORT_REQUIRED ")
                        .append(stock.stockCode()).append(" earnings analysis DATA_INSUFFICIENT"));
        if (dartFinancialImportHistoryPort == null) {
            return;
        }
        stocks.forEach(stock -> dartFinancialImportHistoryPort.findHistoriesByStockCode(stock.stockCode()).stream()
                .findFirst()
                .ifPresent(history -> {
                    if (history.status() == DartFinancialImportStatus.FAILED) {
                        result.append("\n- DART_IMPORT_FAILED ")
                                .append(stock.stockCode()).append(" ")
                                .append(history.failureReason());
                    }
                    latestEarnings(stock.stockCode())
                            .filter(earnings -> earnings.status() == EarningsAnalysisStatus.STRONG
                                    || earnings.status() == EarningsAnalysisStatus.WEAK)
                            .ifPresent(earnings -> {
                                if (history.status() == DartFinancialImportStatus.SUCCESS
                                        || history.status() == DartFinancialImportStatus.PARTIAL) {
                                    result.append("\n- DART_IMPORT_RECENT_EARNINGS_STATUS ")
                                            .append(stock.stockCode())
                                            .append(" ").append(earnings.status())
                                            .append(" overallScore=").append(earnings.overallScore());
                                }
                            });
                }));
    }

    private List<EarningsEvent> upcomingEarningsEvents(LocalDate from, LocalDate to) {
        if (earningsEventPort == null) {
            return List.of();
        }
        return earningsEventPort.findByStatusAndExpectedAnnouncementDateBetween(
                EarningsEventStatus.SCHEDULED, from, to);
    }

    private List<EarningsPreview> readyPreviews(LocalDate from, LocalDate to) {
        if (earningsPreviewPort == null) {
            return List.of();
        }
        return earningsPreviewPort.findByStatusAndPreviewDateBetween(EarningsPreviewStatus.READY, from, to);
    }

    private List<EarningsEvent> announcedNotReviewed(LocalDate from, LocalDate to) {
        if (earningsEventPort == null || postEarningsReviewPort == null) {
            return List.of();
        }
        return earningsEventPort.find(null, from, to).stream()
                .filter(event -> event.status() == EarningsEventStatus.ANNOUNCED)
                .filter(event -> postEarningsReviewPort.findByEarningsEventId(event.id()).isEmpty())
                .toList();
    }

    private List<PostEarningsReview> negativeReviews() {
        if (postEarningsReviewPort == null) {
            return List.of();
        }
        return postEarningsReviewPort.findByThesisImpactIn(List.of(ThesisImpact.WEAKENED, ThesisImpact.BROKEN));
    }

    private String earningsStatus(String stockCode) {
        return latestEarnings(stockCode)
                .map(value -> value.status() + "(overallScore=" + value.overallScore() + ")")
                .orElse("DATA_INSUFFICIENT");
    }

    private Optional<EarningsAnalysisSnapshot> latestEarnings(String stockCode) {
        if (earningsAnalysisPort == null) {
            return Optional.empty();
        }
        return earningsAnalysisPort.findLatestByStockCode(stockCode);
    }

    private boolean technicallyFavorable(String stockCode, LocalDate tradeDate) {
        Optional<DailyPrice> price = latestPrice(stockCode, tradeDate);
        Optional<IndicatorSnapshot> indicator = latestIndicator(stockCode, tradeDate);
        return price.isPresent() && indicator.isPresent() && !incomplete(indicator.get())
                && price.get().closePrice().compareTo(indicator.get().ma20()) > 0
                && indicator.get().ma20().compareTo(indicator.get().ma60()) > 0;
    }

    private Optional<DailyPrice> latestPrice(String stockCode, LocalDate tradeDate) {
        return dailyPricePort.findByStockCodeAndTradeDateBetween(
                        stockCode, tradeDate.minusDays(LOOKBACK_DAYS), tradeDate)
                .stream().max(Comparator.comparing(DailyPrice::tradeDate));
    }

    private Optional<IndicatorSnapshot> latestIndicator(String stockCode, LocalDate tradeDate) {
        return indicatorPort.findByStockCodeAndTradeDateBetween(
                        stockCode, tradeDate.minusDays(LOOKBACK_DAYS), tradeDate)
                .stream().max(Comparator.comparing(IndicatorSnapshot::tradeDate));
    }

    private static boolean incomplete(IndicatorSnapshot value) {
        return value.ma20() == null || value.ma60() == null || value.rsi14() == null
                || value.macd() == null || value.macdSignal() == null
                || value.bollingerUpper() == null || value.bollingerLower() == null;
    }

    private static boolean greaterThan(BigDecimal value, String threshold) {
        return value != null && value.compareTo(new BigDecimal(threshold)) > 0;
    }

    private static String side(BigDecimal value, BigDecimal reference) {
        int comparison = value.compareTo(reference);
        return comparison > 0 ? "ABOVE" : comparison < 0 ? "BELOW" : "EQUAL";
    }

    private static String rsiState(BigDecimal rsi) {
        if (rsi.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "OVERBOUGHT(" + rsi + ")";
        }
        if (rsi.compareTo(BigDecimal.valueOf(30)) <= 0) {
            return "OVERSOLD(" + rsi + ")";
        }
        return "NEUTRAL(" + rsi + ")";
    }

    private static String macdState(IndicatorSnapshot value) {
        return value.macd().compareTo(value.macdSignal()) >= 0 ? "BULLISH" : "BEARISH";
    }

    private static String bollingerState(BigDecimal close, IndicatorSnapshot value) {
        if (close.compareTo(value.bollingerUpper()) >= 0) {
            return "ABOVE_UPPER";
        }
        if (close.compareTo(value.bollingerLower()) <= 0) {
            return "BELOW_LOWER";
        }
        return "INSIDE";
    }
}
