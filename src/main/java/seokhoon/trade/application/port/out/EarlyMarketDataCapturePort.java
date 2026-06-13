package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.EarlyMarketDataCapture;

import java.time.LocalDate;
import java.util.List;

public interface EarlyMarketDataCapturePort {
    EarlyMarketDataCapture save(EarlyMarketDataCapture capture);

    List<EarlyMarketDataCapture> findCaptures(LocalDate tradeDate);
}
