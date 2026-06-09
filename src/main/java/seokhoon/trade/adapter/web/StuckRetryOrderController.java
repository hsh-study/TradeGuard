package seokhoon.trade.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.LoadStuckRetryOrdersUseCase;
import seokhoon.trade.application.port.in.OrderRequestView;
import seokhoon.trade.application.port.in.RecoverStuckRetryOrderUseCase;
import seokhoon.trade.config.OrderRetryProperties;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/mock-orders")
public class StuckRetryOrderController {
    private final LoadStuckRetryOrdersUseCase loadStuckRetryOrdersUseCase;
    private final RecoverStuckRetryOrderUseCase recoverStuckRetryOrderUseCase;
    private final OrderRetryProperties properties;
    private final Clock clock;

    @Autowired
    public StuckRetryOrderController(
            LoadStuckRetryOrdersUseCase loadStuckRetryOrdersUseCase,
            RecoverStuckRetryOrderUseCase recoverStuckRetryOrderUseCase,
            OrderRetryProperties properties
    ) {
        this(
                loadStuckRetryOrdersUseCase,
                recoverStuckRetryOrderUseCase,
                properties,
                Clock.systemUTC()
        );
    }

    StuckRetryOrderController(
            LoadStuckRetryOrdersUseCase loadStuckRetryOrdersUseCase,
            RecoverStuckRetryOrderUseCase recoverStuckRetryOrderUseCase,
            OrderRetryProperties properties,
            Clock clock
    ) {
        this.loadStuckRetryOrdersUseCase = loadStuckRetryOrdersUseCase;
        this.recoverStuckRetryOrderUseCase = recoverStuckRetryOrderUseCase;
        this.properties = properties;
        this.clock = clock;
    }

    @GetMapping("/retries/stuck")
    List<StuckRetryOrderResponse> findStuckRetries(
            @RequestParam(required = false) @Min(1) Long thresholdMinutes
    ) {
        Duration threshold = threshold(thresholdMinutes);
        return loadStuckRetryOrdersUseCase.load(clock.instant(), threshold).stream()
                .map(StuckRetryOrderResponse::from)
                .toList();
    }

    @PostMapping("/{orderId}/retry/recover")
    StuckRetryOrderResponse recover(
            @PathVariable long orderId,
            @Valid @RequestBody RecoverStuckRetryRequest request
    ) {
        return StuckRetryOrderResponse.from(recoverStuckRetryOrderUseCase.recover(
                orderId,
                request.reason(),
                clock.instant(),
                threshold(null)
        ));
    }

    private Duration threshold(Long thresholdMinutes) {
        long minutes = thresholdMinutes == null
                ? properties.getRetryStuckThresholdMinutes()
                : thresholdMinutes;
        return Duration.ofMinutes(minutes);
    }

    public record RecoverStuckRetryRequest(@NotBlank String reason) {
    }

    public record StuckRetryOrderResponse(
            Long orderId,
            Long signalId,
            String stockCode,
            String strategyName,
            LocalDate tradeDate,
            OrderStatus status,
            Instant retryRequestedAt,
            String failureReason,
            Instant failedAt,
            boolean retryable
    ) {
        static StuckRetryOrderResponse from(OrderRequestView order) {
            return new StuckRetryOrderResponse(
                    order.orderId(),
                    order.signalId(),
                    order.stockCode(),
                    order.strategyName(),
                    order.tradeDate(),
                    order.status(),
                    order.retryRequestedAt(),
                    order.failureReason(),
                    order.failedAt(),
                    order.retryable()
            );
        }
    }
}
