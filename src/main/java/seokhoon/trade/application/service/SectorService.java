package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.ResearchUseCases.AddSectorStockCommand;
import seokhoon.trade.application.port.in.ResearchUseCases.CreateSectorCommand;
import seokhoon.trade.application.port.in.ResearchUseCases.SectorSnapshotGenerationResult;
import seokhoon.trade.application.port.in.ResearchUseCases.SectorUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.market.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class SectorService implements SectorUseCase {
    private static final int PRICE_LOOKBACK_DAYS = 10;

    private final SectorPort sectorPort;
    private final StockSectorMappingPort mappingPort;
    private final SectorDailySnapshotPort snapshotPort;
    private final DailyPricePort dailyPricePort;
    private final MarketCalendarPort calendarPort;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public SectorService(
            SectorPort sectorPort,
            StockSectorMappingPort mappingPort,
            SectorDailySnapshotPort snapshotPort,
            DailyPricePort dailyPricePort,
            MarketCalendarPort calendarPort,
            OperationalMetricsPort metrics
    ) {
        this(sectorPort, mappingPort, snapshotPort, dailyPricePort, calendarPort,
                metrics, Clock.systemUTC());
    }

    SectorService(
            SectorPort sectorPort,
            StockSectorMappingPort mappingPort,
            SectorDailySnapshotPort snapshotPort,
            DailyPricePort dailyPricePort,
            MarketCalendarPort calendarPort,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.sectorPort = sectorPort;
        this.mappingPort = mappingPort;
        this.snapshotPort = snapshotPort;
        this.dailyPricePort = dailyPricePort;
        this.calendarPort = calendarPort;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    public Sector create(CreateSectorCommand command) {
        Objects.requireNonNull(command, "command");
        return sectorPort.save(new Sector(null, command.sectorCode(), command.sectorName(),
                command.sectorType() == null ? SectorType.CUSTOM : command.sectorType(),
                clock.instant(), clock.instant()));
    }

    @Override
    public StockSectorMapping addStock(String sectorCode, AddSectorStockCommand command) {
        Objects.requireNonNull(command, "command");
        requireSector(sectorCode);
        return mappingPort.save(new StockSectorMapping(null, command.stockCode(), sectorCode,
                command.source() == null || command.source().isBlank() ? "MANUAL" : command.source(),
                clock.instant(), clock.instant()));
    }

    @Override
    public List<Sector> findAll() {
        return sectorPort.findAll();
    }

    @Override
    public SectorDailySnapshot loadSnapshot(String sectorCode, LocalDate tradeDate) {
        return snapshotPort.findBySectorCodeAndTradeDate(sectorCode, tradeDate)
                .orElseThrow(() -> new ResearchNotFoundException(
                        "Sector snapshot not found: " + sectorCode + " " + tradeDate));
    }

    @Override
    public SectorSnapshotGenerationResult generateSnapshots(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        try {
            List<Sector> sectors = sectorPort.findAll();
            if (sectors.isEmpty()) {
                metrics.recordResearchSectorSnapshot("no_data");
                return new SectorSnapshotGenerationResult(tradeDate, 0, 0, 0);
            }
            int generated = 0;
            int insufficient = 0;
            for (Sector sector : sectors) {
                SectorDailySnapshot snapshot = generateSnapshot(sector.sectorCode(), tradeDate);
                generated++;
                if (snapshot.dataInsufficient()) {
                    insufficient++;
                }
            }
            metrics.recordResearchSectorSnapshot(generated == 0 ? "no_data" : "success");
            return new SectorSnapshotGenerationResult(tradeDate, sectors.size(), generated, insufficient);
        } catch (RuntimeException exception) {
            metrics.recordResearchSectorSnapshot("failure");
            throw exception;
        }
    }

    private SectorDailySnapshot generateSnapshot(String sectorCode, LocalDate tradeDate) {
        List<SectorStockMove> moves = mappingPort.findBySectorCode(sectorCode).stream()
                .map(mapping -> stockMove(mapping.stockCode(), tradeDate))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(SectorStockMove::changeRate))
                .toList();
        SectorDailySnapshot snapshot = moves.isEmpty()
                ? insufficientSnapshot(sectorCode, tradeDate)
                : calculatedSnapshot(sectorCode, tradeDate, moves);
        return snapshotPort.save(snapshot);
    }

    private Optional<SectorStockMove> stockMove(String stockCode, LocalDate tradeDate) {
        LocalDate previousTradingDay = calendarPort.previousTradingDay(tradeDate);
        Optional<DailyPrice> today = latestPrice(stockCode, tradeDate, tradeDate);
        Optional<DailyPrice> previous = latestPrice(stockCode,
                previousTradingDay.minusDays(PRICE_LOOKBACK_DAYS), previousTradingDay);
        if (today.isEmpty() || previous.isEmpty() || previous.get().closePrice().signum() == 0) {
            return Optional.empty();
        }
        BigDecimal changeRate = today.get().closePrice()
                .subtract(previous.get().closePrice())
                .divide(previous.get().closePrice(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
        return Optional.of(new SectorStockMove(stockCode, changeRate, today.get().tradingValue()));
    }

    private Optional<DailyPrice> latestPrice(String stockCode, LocalDate from, LocalDate to) {
        return dailyPricePort.findByStockCodeAndTradeDateBetween(stockCode, from, to)
                .stream().max(Comparator.comparing(DailyPrice::tradeDate));
    }

    private SectorDailySnapshot calculatedSnapshot(
            String sectorCode,
            LocalDate tradeDate,
            List<SectorStockMove> moves
    ) {
        BigDecimal totalChange = moves.stream()
                .map(SectorStockMove::changeRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = totalChange.divide(BigDecimal.valueOf(moves.size()), 4, RoundingMode.HALF_UP);
        BigDecimal median = median(moves);
        BigDecimal tradingValue = moves.stream()
                .map(SectorStockMove::tradingValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        SectorStockMove leading = moves.stream()
                .max(Comparator.comparing(SectorStockMove::changeRate)
                        .thenComparing(SectorStockMove::tradingValue))
                .orElseThrow();
        int rising = (int) moves.stream().filter(move -> move.changeRate().signum() > 0).count();
        int falling = (int) moves.stream().filter(move -> move.changeRate().signum() < 0).count();
        return new SectorDailySnapshot(null, sectorCode, tradeDate, average, median,
                tradingValue, rising, falling, leading.stockCode(), leading.changeRate(),
                clock.instant(), clock.instant());
    }

    private static BigDecimal median(List<SectorStockMove> moves) {
        int middle = moves.size() / 2;
        if (moves.size() % 2 == 1) {
            return moves.get(middle).changeRate();
        }
        return moves.get(middle - 1).changeRate()
                .add(moves.get(middle).changeRate())
                .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    private SectorDailySnapshot insufficientSnapshot(String sectorCode, LocalDate tradeDate) {
        return new SectorDailySnapshot(null, sectorCode, tradeDate, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, null, null,
                clock.instant(), clock.instant());
    }

    private void requireSector(String sectorCode) {
        sectorPort.findBySectorCode(sectorCode)
                .orElseThrow(() -> new ResearchNotFoundException("Sector not found: " + sectorCode));
    }

    private record SectorStockMove(String stockCode, BigDecimal changeRate, BigDecimal tradingValue) {
    }
}
