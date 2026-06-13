package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.EarlyMarketCaptureStatus;
import seokhoon.trade.domain.market.EarlyMarketDataCapture;

import java.time.LocalDate;
import java.util.List;

public record EarlyMarketDataCaptureResult(
        LocalDate tradeDate,
        List<EarlyMarketDataCapture> captures
) {
    public boolean hasFailure() {
        return captures.stream().anyMatch(capture ->
                capture.status() == EarlyMarketCaptureStatus.FAILED
                        || capture.status() == EarlyMarketCaptureStatus.PARTIAL
        );
    }

    public int itemCount() {
        return captures.stream()
                .mapToInt(EarlyMarketDataCapture::itemCount)
                .sum();
    }
}
