package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.indicator.*;
import seokhoon.trade.domain.stock.Stock;

import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/indicators")
public class IndicatorWarmUpController {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final WarmUpDailyPricesAndIndicatorsUseCase warmUpUseCase;
    private final FindStocksUseCase findStocksUseCase;
    private final IndicatorWarmUpHistoryPort historyPort;
    private final IndicatorSnapshotPort snapshotPort;
    private final Clock clock;

    @Autowired
    public IndicatorWarmUpController(
            WarmUpDailyPricesAndIndicatorsUseCase warmUpUseCase,
            FindStocksUseCase findStocksUseCase,
            IndicatorWarmUpHistoryPort historyPort,
            IndicatorSnapshotPort snapshotPort
    ) {
        this(warmUpUseCase, findStocksUseCase, historyPort, snapshotPort,
                Clock.system(SEOUL));
    }

    IndicatorWarmUpController(
            WarmUpDailyPricesAndIndicatorsUseCase warmUpUseCase,
            FindStocksUseCase findStocksUseCase,
            IndicatorWarmUpHistoryPort historyPort,
            IndicatorSnapshotPort snapshotPort,
            Clock clock
    ) {
        this.warmUpUseCase = warmUpUseCase;
        this.findStocksUseCase = findStocksUseCase;
        this.historyPort = historyPort;
        this.snapshotPort = snapshotPort;
        this.clock = clock;
    }

    @PostMapping("/warm-up")
    WarmUpResponse warmUp(
            @RequestParam String stockCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate baseDate
    ) {
        return WarmUpResponse.from(warmUpUseCase.warmUpStock(
                stockCode, date(baseDate)));
    }

    @PostMapping("/warm-up/active-stocks")
    List<WarmUpResponse> warmUpActiveStocks(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate baseDate
    ) {
        List<String> stockCodes = findStocksUseCase.findAll().stream()
                .filter(Stock::active)
                .map(Stock::stockCode)
                .toList();
        return warmUpUseCase.warmUpStocks(stockCodes, date(baseDate))
                .stream()
                .map(WarmUpResponse::from)
                .toList();
    }

    @GetMapping("/warm-up/histories")
    List<WarmUpHistoryResponse> histories(
            @RequestParam String stockCode
    ) {
        return historyPort.findByStockCode(stockCode).stream()
                .map(WarmUpHistoryResponse::from)
                .toList();
    }

    @GetMapping("/snapshots")
    List<IndicatorSnapshot> snapshots(
            @RequestParam String stockCode,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tradeDate
    ) {
        return snapshotPort.findByStockCodeAndTradeDateBetween(
                stockCode, tradeDate, tradeDate);
    }

    private LocalDate date(LocalDate value) {
        return value == null ? LocalDate.now(clock) : value;
    }

    public record WarmUpResponse(
            String stockCode,
            LocalDate baseDate,
            LocalDate requestedFrom,
            LocalDate requestedTo,
            int importedDailyPriceCount,
            int totalDailyPriceCount,
            boolean indicatorCalculated,
            boolean sufficientForMa20,
            boolean sufficientForMa60,
            List<String> warnings,
            IndicatorWarmUpStatus status
    ) {
        static WarmUpResponse from(IndicatorWarmUpResult result) {
            return new WarmUpResponse(
                    result.stockCode(),
                    result.baseDate(),
                    result.requestedFrom(),
                    result.requestedTo(),
                    result.importedDailyPriceCount(),
                    result.totalDailyPriceCount(),
                    result.indicatorCalculated(),
                    result.sufficientForMa20(),
                    result.sufficientForMa60(),
                    result.warnings(),
                    result.status()
            );
        }
    }

    record WarmUpHistoryResponse(
            Long id,
            String stockCode,
            LocalDate baseDate,
            IndicatorWarmUpStatus status,
            int importedDailyPriceCount,
            int totalDailyPriceCount,
            boolean sufficientForMa20,
            boolean sufficientForMa60,
            String failureReason,
            Instant createdAt
    ) {
        static WarmUpHistoryResponse from(
                IndicatorWarmUpHistory history
        ) {
            return new WarmUpHistoryResponse(
                    history.id(),
                    history.stockCode(),
                    history.baseDate(),
                    history.status(),
                    history.importedDailyPriceCount(),
                    history.totalDailyPriceCount(),
                    history.sufficientForMa20(),
                    history.sufficientForMa60(),
                    history.failureReason(),
                    history.createdAt()
            );
        }
    }
}
