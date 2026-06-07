package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.LoadOrderRequestsUseCase;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.in.RequestStoredMockOrderUseCase;
import seokhoon.trade.application.port.in.StoredMockOrderCommand;
import seokhoon.trade.application.service.TradingSignalNotFoundException;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
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
    MockOrderResponse request(@RequestBody MockOrderRequest request) {
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
            @RequestParam(required = false) OrderStatus status
    ) {
        return loadOrderRequestsUseCase.load(stockCode, tradeDate, status).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @ExceptionHandler(TradingSignalNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse handleSignalNotFound(TradingSignalNotFoundException exception) {
        return new ErrorResponse("TRADING_SIGNAL_NOT_FOUND", exception.getMessage());
    }

    public record MockOrderRequest(
            String strategyName,
            String stockCode,
            LocalDate signalDate,
            SignalType signalType,
            int quantity,
            BigDecimal limitPrice
    ) {
    }

    public record MockOrderResponse(
            boolean approved,
            List<String> rejectionReasons,
            TradingSignalStatus signalStatus,
            OrderResponse order
    ) {
        static MockOrderResponse from(MockOrderResult result) {
            return new MockOrderResponse(
                    result.riskDecision().approved(),
                    result.riskDecision().reasons(),
                    result.tradingSignal().status(),
                    result.orderRequest() == null ? null : OrderResponse.from(result.orderRequest())
            );
        }
    }

    public record OrderResponse(
            String stockCode,
            OrderSide side,
            OrderType orderType,
            int quantity,
            BigDecimal limitPrice,
            OrderStatus status,
            String brokerOrderNo,
            String strategyName,
            LocalDate tradeDate
    ) {
        static OrderResponse from(OrderRequest orderRequest) {
            return new OrderResponse(
                    orderRequest.stockCode(),
                    orderRequest.side(),
                    orderRequest.orderType(),
                    orderRequest.quantity(),
                    orderRequest.limitPrice(),
                    orderRequest.status(),
                    orderRequest.brokerOrderNo(),
                    orderRequest.strategyName(),
                    orderRequest.tradeDate()
            );
        }
    }

    public record ErrorResponse(String code, String message) {
    }
}
