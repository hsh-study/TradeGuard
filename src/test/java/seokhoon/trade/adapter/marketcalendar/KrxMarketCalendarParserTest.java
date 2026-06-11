package seokhoon.trade.adapter.marketcalendar;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class KrxMarketCalendarParserTest {
    @Test
    void mapsKrxStyleCalendarRowsWithoutLoggingRawPayload() {
        KrxMarketCalendarParser parser =
                new KrxMarketCalendarParser(new ObjectMapper());

        var result = parser.parse("""
                {
                  "OutBlock_1": [
                    {"calnd_dd":"20260102","opn_yn":"Y","holdy_nm":""},
                    {"calnd_dd":"20260101","opn_yn":"N","holdy_nm":"신정"}
                  ]
                }
                """, 2026);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).tradingDay()).isTrue();
        assertThat(result.get(1).holidayName()).isEqualTo("신정");
        assertThat(result.get(1).source().name()).isEqualTo("KRX_OFFICIAL");
    }
}
