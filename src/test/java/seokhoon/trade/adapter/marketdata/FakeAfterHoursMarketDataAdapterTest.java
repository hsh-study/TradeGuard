package seokhoon.trade.adapter.marketdata;

import org.junit.jupiter.api.Test;
import seokhoon.trade.domain.market.AfterHoursQuote;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FakeAfterHoursMarketDataAdapterTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 9);

    @Test
    void returnsInjectedQuotesOrderedByChangeRate() {
        FakeAfterHoursMarketDataAdapter adapter = new FakeAfterHoursMarketDataAdapter(List.of(
                quote("005930", "3.0", "30000000000"),
                quote("000660", "6.0", "50000000000")
        ));

        List<AfterHoursQuote> result = adapter.findTopAfterHoursMovers(TRADE_DATE, 1);

        assertThat(result).singleElement().satisfies(quote -> {
            assertThat(quote.stockCode()).isEqualTo("000660");
            assertThat(quote.tradeDate()).isEqualTo(TRADE_DATE);
            assertThat(quote.capturedAt()).isEqualTo(Instant.parse("2026-06-09T09:30:00Z"));
        });
    }

    @Test
    void findsQuoteByStockCodeAndReturnsEmptyForUnknownStock() {
        FakeAfterHoursMarketDataAdapter adapter =
                new FakeAfterHoursMarketDataAdapter(List.of(
                        quote("005930", "3.0", "30000000000")
                ));

        assertThat(adapter.findByStockCode("005930", TRADE_DATE))
                .get()
                .extracting(AfterHoursQuote::afterHoursChangeRate)
                .isEqualTo(new BigDecimal("3.0"));
        assertThat(adapter.findByStockCode("999999", TRADE_DATE)).isEmpty();
    }

    private static AfterHoursQuote quote(
            String stockCode,
            String changeRate,
            String tradingValue
    ) {
        return new AfterHoursQuote(
                stockCode,
                "stock-" + stockCode,
                TRADE_DATE,
                BigDecimal.valueOf(50_000),
                new BigDecimal(changeRate),
                100_000,
                new BigDecimal(tradingValue),
                Instant.parse("2026-06-09T09:30:00Z")
        );
    }
}
