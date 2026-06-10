package seokhoon.trade.adapter.marketdata;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.IntradayBarPort;
import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.IntradayBar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        name = "tradeguard.market-data.intraday-provider",
        havingValue = "fake",
        matchIfMissing = true
)
public class FakeIntradayBarAdapter implements IntradayBarPort {
    private static final LocalDate DEFAULT_TRADE_DATE = LocalDate.of(2026, 6, 10);
    private static final List<IntradayBar> DEFAULT_BARS = List.of(
            bar("005930", "09:00", "75000", "75300", "74800", "75200", "75100"),
            bar("005930", "09:05", "75200", "76500", "75100", "76200", "75600"),
            bar("005930", "09:30", "76200", "77250", "76000", "77000", "76300"),
            bar("000660", "09:00", "183000", "184000", "181000", "182000", "182500"),
            bar("000660", "09:05", "182000", "182500", "175000", "176000", "179000"),
            bar("000660", "09:30", "176000", "177000", "170190", "172000", "175000"),
            bar("035420", "09:00", "218000", "219000", "217000", "218500", "218200"),
            bar("035420", "09:05", "218500", "219500", "218000", "219000", "218700"),
            bar("035420", "09:30", "219000", "219200", "217800", "218200", "218500")
    );

    private final List<IntradayBar> bars;
    private final boolean alignDefaultBarsToRequestedDate;

    public FakeIntradayBarAdapter() {
        this(DEFAULT_BARS, true);
    }

    public FakeIntradayBarAdapter(List<IntradayBar> bars) {
        this(bars, false);
    }

    private FakeIntradayBarAdapter(
            List<IntradayBar> bars,
            boolean alignDefaultBarsToRequestedDate
    ) {
        this.bars = List.copyOf(Objects.requireNonNull(bars, "bars"));
        this.alignDefaultBarsToRequestedDate = alignDefaultBarsToRequestedDate;
    }

    @Override
    public List<IntradayBar> findBars(
            String stockCode,
            LocalDate tradeDate,
            LocalTime from,
            LocalTime to,
            BarInterval interval
    ) {
        validate(stockCode, tradeDate, from, to, interval);
        return bars.stream()
                .map(bar -> alignDefaultBarsToRequestedDate
                        ? withTradeDate(bar, tradeDate)
                        : bar)
                .filter(bar -> bar.stockCode().equals(stockCode))
                .filter(bar -> bar.tradeDate().equals(tradeDate))
                .filter(bar -> !bar.barTime().isBefore(from))
                .filter(bar -> !bar.barTime().isAfter(to))
                .filter(bar -> matchesInterval(bar.barTime(), interval))
                .sorted(Comparator.comparing(IntradayBar::barTime))
                .toList();
    }

    private static IntradayBar withTradeDate(IntradayBar bar, LocalDate tradeDate) {
        return new IntradayBar(
                bar.stockCode(),
                tradeDate,
                bar.barTime(),
                bar.openPrice(),
                bar.highPrice(),
                bar.lowPrice(),
                bar.closePrice(),
                bar.volume(),
                bar.tradingValue(),
                bar.vwap()
        );
    }

    private static boolean matchesInterval(LocalTime barTime, BarInterval interval) {
        return interval == BarInterval.ONE_MINUTE || barTime.getMinute() % 5 == 0;
    }

    private static void validate(
            String stockCode,
            LocalDate tradeDate,
            LocalTime from,
            LocalTime to,
            BarInterval interval
    ) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        Objects.requireNonNull(tradeDate, "tradeDate");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(interval, "interval");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }

    private static IntradayBar bar(
            String stockCode,
            String barTime,
            String openPrice,
            String highPrice,
            String lowPrice,
            String closePrice,
            String vwap
    ) {
        BigDecimal close = new BigDecimal(closePrice);
        return new IntradayBar(
                stockCode,
                DEFAULT_TRADE_DATE,
                LocalTime.parse(barTime),
                new BigDecimal(openPrice),
                new BigDecimal(highPrice),
                new BigDecimal(lowPrice),
                close,
                100_000,
                close.multiply(BigDecimal.valueOf(100_000)),
                new BigDecimal(vwap)
        );
    }
}
