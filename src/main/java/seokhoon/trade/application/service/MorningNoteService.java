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
            OperationalMetricsPort metrics
    ) {
        this(notePort, stockPort, dailyPricePort, indicatorPort, signalPort, positionPort,
                thesisPort, catalystPort, calendarPort, marketIndexPort, sectorPort,
                stockSectorMappingPort, sectorSnapshotPort, metrics, Clock.systemUTC());
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
                    actionItems(catalysts, brokenTheses, stocks, tradeDate),
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
                    .append(" avg=").append(position.averageBuyPrice());
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
                + " Bollinger=" + bollingerState(close, value);
    }

    private String actionItems(
            List<InvestmentCatalyst> catalysts,
            List<InvestmentThesis> brokenTheses,
            List<Stock> stocks,
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
        if (catalysts.isEmpty() && brokenTheses.isEmpty() && stocks.isEmpty()) {
            result.append("\n- 등록된 리서치 대상 없음");
        }
        return result.toString();
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
