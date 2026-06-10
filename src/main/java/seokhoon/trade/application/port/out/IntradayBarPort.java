package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.IntradayBar;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface IntradayBarPort {
    List<IntradayBar> findBars(
            String stockCode,
            LocalDate tradeDate,
            LocalTime from,
            LocalTime to,
            BarInterval interval
    );
}
