package seokhoon.trade.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.LoadOrderRequestsUseCase;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.in.OrderRequestView;
import seokhoon.trade.application.port.in.RequestStoredMockOrderUseCase;
import seokhoon.trade.application.port.in.StoredMockOrderCommand;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/mock-orders")
public class MockOrderController {
    private final RequestStoredMockOrderUseCase requestStoredMockOrderUseCase;
    private final LoadOrderRequestsUseCase loadOrderRequestsUseCase;

    public MockOrderController(
            RequestStoredMockOrderUseCase requestStoredMockOrderUseCase,
            LoadOrderRequestsUseCase loadOrderRequestsUseCase
    ) {
        this.requestStoredMockOrderUseCase = requestStoredMockOrderUseCase;
        this.loadOrderRequestsUseCase = loadOrderRequestsUseCase;
    }

    @PostMapping
    MockOrderResponse request(@Valid @RequestBody MockOrderRequest request) {
        MockOrderResult result = requestStoredMockOrderUseCase.request(new StoredMockOrderCommand(
                request.strategyName(),
                request.stockCode(),
                request.signalDate(),
                request.signalType(),
                request.quantity(),
                request.limitPrice()
        ));
        return MockOrderResponse.from(result);
    }

    @GetMapping
    List<OrderResponse> find(
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderSide side
    ) {
        return loadOrderRequestsUseCase.load(stockCode, tradeDate, status, side).stream()
                .map(OrderResponse::from)
                .toList();
    }

    public record MockOrderRequest(
            @NotBlank String strategyName,
            @NotBlank String stockCode,
            @NotNull LocalDate signalDate,
            @NotNull SignalType signalType,
            @Min(1) int quantity,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal limitPrice
    ) {
    }

    public record MockOrderResponse(
            boolean approved,
            boolean brokerFailed,
            String failureReason,
            List<String> rejectionReasons,
            TradingSignalStatus signalStatus,
            OrderResponse order
    ) {
        static MockOrderResponse from(MockOrderResult result) {
            return new MockOrderResponse(
                    result.riskDecision().approved() && !result.brokerFailed(),
                    result.brokerFailed(),
                    result.failureReason(),
                    result.riskDecision().reasons(),
                    result.tradingSignal().status(),
                    result.orderRequest() == null ? null : OrderResponse.from(result.orderRequest())
            );
        }
    }

    public record OrderResponse(
            Long orderId,
            String stockCode,
            OrderSide side,
            OrderType orderType,
            int quantity,
            BigDecimal limitPrice,
            OrderStatus status,
            String brokerOrderNo,
            String failureReason,
            Instant failedAt,
            boolean retryable,
            String strategyName,
            LocalDate tradeDate
    ) {
        static OrderResponse from(OrderRequestView orderRequest) {
            return new OrderResponse(
                    orderRequest.orderId(),
                    orderRequest.stockCode(),
                    orderRequest.side(),
                    orderRequest.orderType(),
                    orderRequest.quantity(),
                    orderRequest.limitPrice(),
                    orderRequest.status(),
                    orderRequest.brokerOrderNo(),
                    orderRequest.failureReason(),
                    orderRequest.failedAt(),
                    orderRequest.retryable(),
                    orderRequest.strategyName(),
                    orderRequest.tradeDate()
            );
        }

        static OrderResponse from(OrderRequest orderRequest) {
            return new OrderResponse(
                    null,
                    orderRequest.stockCode(),
                    orderRequest.side(),
                    orderRequest.orderType(),
                    orderRequest.quantity(),
                    orderRequest.limitPrice(),
                    orderRequest.status(),
                    orderRequest.brokerOrderNo(),
                    orderRequest.failureReason(),
                    orderRequest.failedAt(),
                    orderRequest.retryable(),
                    orderRequest.strategyName(),
                    orderRequest.tradeDate()
            );
        }
    }
}
