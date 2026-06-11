package seokhoon.trade.adapter.marketdata;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.AfterHoursMarketDataPort;
import seokhoon.trade.domain.market.AfterHoursQuote;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnAfterHoursProvider("fake")
public class FakeAfterHoursMarketDataAdapter implements AfterHoursMarketDataPort {
    private static final Instant DEFAULT_CAPTURED_AT =
            Instant.parse("2026-06-09T09:30:00Z");
    private static final List<QuoteTemplate> DEFAULT_QUOTES = List.of(
            template("005930", "삼성전자", "76000", "3.5", 850_000, "42000000000"),
            template("000660", "SK하이닉스", "183000", "5.2", 320_000, "58000000000"),
            template("035420", "NAVER", "218000", "-3.4", 95_000, "12000000000"),
            template("035720", "카카오", "62000", "7.8", 510_000, "31000000000"),
            template("247540", "에코프로비엠", "212000", "2.2", 170_000, "26000000000")
    );

    private final List<QuoteTemplate> quotes;

    public FakeAfterHoursMarketDataAdapter() {
        this.quotes = DEFAULT_QUOTES;
    }

    FakeAfterHoursMarketDataAdapter(List<AfterHoursQuote> quotes) {
        this.quotes = quotes.stream().map(QuoteTemplate::from).toList();
    }

    @Override
    public List<AfterHoursQuote> findTopAfterHoursMovers(LocalDate tradeDate, int limit) {
        validate(tradeDate, limit);
        return quotes.stream()
                .sorted(Comparator.comparing(
                        QuoteTemplate::afterHoursChangeRate,
                        Comparator.reverseOrder()
                ))
                .limit(limit)
                .map(template -> template.toQuote(tradeDate))
                .toList();
    }

    @Override
    public Optional<AfterHoursQuote> findByStockCode(
            String stockCode,
            LocalDate tradeDate
    ) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        if (tradeDate == null) {
            throw new IllegalArgumentException("tradeDate must not be null");
        }
        return quotes.stream()
                .filter(template -> template.stockCode().equals(stockCode))
                .findFirst()
                .map(template -> template.toQuote(tradeDate));
    }

    private static void validate(LocalDate tradeDate, int limit) {
        if (tradeDate == null) {
            throw new IllegalArgumentException("tradeDate must not be null");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
    }

    private static QuoteTemplate template(
            String stockCode,
            String stockName,
            String price,
            String changeRate,
            long volume,
            String tradingValue
    ) {
        return new QuoteTemplate(
                stockCode,
                stockName,
                new BigDecimal(price),
                new BigDecimal(changeRate),
                volume,
                new BigDecimal(tradingValue),
                DEFAULT_CAPTURED_AT
        );
    }

    private record QuoteTemplate(
            String stockCode,
            String stockName,
            BigDecimal afterHoursPrice,
            BigDecimal afterHoursChangeRate,
            long afterHoursVolume,
            BigDecimal afterHoursTradingValue,
            Instant capturedAt
    ) {
        static QuoteTemplate from(AfterHoursQuote quote) {
            return new QuoteTemplate(
                    quote.stockCode(),
                    quote.stockName(),
                    quote.afterHoursPrice(),
                    quote.afterHoursChangeRate(),
                    quote.afterHoursVolume(),
                    quote.afterHoursTradingValue(),
                    quote.capturedAt()
            );
        }

        AfterHoursQuote toQuote(LocalDate tradeDate) {
            return new AfterHoursQuote(
                    stockCode,
                    stockName,
                    tradeDate,
                    afterHoursPrice,
                    afterHoursChangeRate,
                    afterHoursVolume,
                    afterHoursTradingValue,
                    capturedAt
            );
        }
    }
}
