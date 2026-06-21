package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;

class KisWebSocketOrderBookParserTest {
    @Test
    void parsesOfficialH0stasp0FieldOrder() {
        String[] fields = new String[59];
        Arrays.fill(fields, "0");
        fields[0] = "005930";
        fields[1] = "101530";
        fields[3] = "70100";
        fields[12] = "71000";
        fields[13] = "69900";
        fields[22] = "69000";
        fields[23] = "12";
        fields[32] = "21";
        fields[33] = "15";
        fields[42] = "24";
        fields[47] = "70000";

        var snapshot = KisWebSocketOrderBookParser.orderBook(String.join("^", fields),
                null, Instant.parse("2026-06-21T01:15:30Z"));

        assertThat(snapshot.stockCode()).isEqualTo("005930");
        assertThat(snapshot.currentPrice()).isEqualByComparingTo("70000");
        assertThat(snapshot.asks()).hasSize(2);
        assertThat(snapshot.asks().getFirst().price()).isEqualByComparingTo("70100");
        assertThat(snapshot.asks().getFirst().quantity()).isEqualTo(12);
        assertThat(snapshot.bids().getFirst().price()).isEqualByComparingTo("69900");
        assertThat(snapshot.bids().getFirst().quantity()).isEqualTo(15);
    }

    @Test
    void executionPriceOverridesAnticipatedPrice() {
        assertThat(KisWebSocketOrderBookParser.executionPrice("005930^101531^70200^2"))
                .isEqualByComparingTo(new BigDecimal("70200"));
    }
}
