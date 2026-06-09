package seokhoon.trade.application.port.in;

import java.time.Duration;
import java.time.Instant;

public interface RecoverStuckRetryOrderUseCase {
    OrderRequestView recover(long orderId, String reason, Instant referenceTime, Duration threshold);
}
