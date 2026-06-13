package seokhoon.trade.domain.market;

import java.time.Instant;
import java.time.LocalDate;

public record EarlyMarketDataCapture(
        Long id,
        LocalDate tradeDate,
        EarlyMarketCaptureType captureType,
        Instant capturedAt,
        String source,
        EarlyMarketCaptureStatus status,
        int itemCount,
        String failureReason,
        Instant createdAt
) {
}
