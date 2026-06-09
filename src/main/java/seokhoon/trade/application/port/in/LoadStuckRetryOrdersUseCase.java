package seokhoon.trade.application.port.in;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface LoadStuckRetryOrdersUseCase {
    List<OrderRequestView> load(Instant referenceTime, Duration threshold);
}
