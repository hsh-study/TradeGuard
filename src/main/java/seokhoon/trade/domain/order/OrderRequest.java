package seokhoon.trade.domain.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class OrderRequest {
    private final String stockCode;
    private final OrderSide side;
    private final OrderType orderType;
    private final int quantity;
    private final BigDecimal limitPrice;
    private OrderStatus status;
    private String brokerOrderNo;
    private String failureReason;
    private Instant failedAt;
    private boolean retryable;
    private final String strategyName;
    private final LocalDate tradeDate;

    public OrderRequest(String stockCode, OrderSide side, OrderType orderType, int quantity, BigDecimal limitPrice,
                        String strategyName, LocalDate tradeDate) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
        if (limitPrice == null || limitPrice.signum() <= 0) {
            throw new IllegalArgumentException("limitPrice must be positive");
        }
        if (orderType != OrderType.LIMIT) {
            throw new IllegalArgumentException("only LIMIT orders are supported");
        }
        this.stockCode = stockCode;
        this.side = Objects.requireNonNull(side, "side");
        this.orderType = orderType;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.status = OrderStatus.CREATED;
        this.strategyName = Objects.requireNonNull(strategyName, "strategyName");
        this.tradeDate = Objects.requireNonNull(tradeDate, "tradeDate");
    }

    public static OrderRequest restore(
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
        OrderRequest orderRequest = new OrderRequest(
                stockCode,
                side,
                orderType,
                quantity,
                limitPrice,
                strategyName,
                tradeDate
        );
        orderRequest.status = Objects.requireNonNull(status, "status");
        orderRequest.brokerOrderNo = brokerOrderNo;
        orderRequest.failureReason = failureReason;
        orderRequest.failedAt = failedAt;
        orderRequest.retryable = retryable;
        return orderRequest;
    }

    public BigDecimal orderAmount() {
        return limitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String stockCode() { return stockCode; }
    public OrderSide side() { return side; }
    public OrderType orderType() { return orderType; }
    public int quantity() { return quantity; }
    public BigDecimal limitPrice() { return limitPrice; }
    public OrderStatus status() { return status; }
    public String brokerOrderNo() { return brokerOrderNo; }
    public String failureReason() { return failureReason; }
    public Instant failedAt() { return failedAt; }
    public boolean retryable() { return retryable; }
    public String strategyName() { return strategyName; }
    public LocalDate tradeDate() { return tradeDate; }

    public void markRequested() { this.status = OrderStatus.REQUESTED; }
    public void accept(String brokerOrderNo) {
        this.status = OrderStatus.ACCEPTED;
        this.brokerOrderNo = brokerOrderNo;
        this.failureReason = null;
        this.failedAt = null;
        this.retryable = false;
    }
    public void reject() { this.status = OrderStatus.REJECTED; }
    public void markBrokerFailed(String reason, Instant failedAt, boolean retryable) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("failure reason must not be blank");
        }
        if (status != OrderStatus.CREATED && status != OrderStatus.REQUESTED) {
            throw new IllegalStateException("broker failure can only be recorded before acceptance");
        }
        this.status = OrderStatus.BROKER_FAILED;
        this.brokerOrderNo = null;
        this.failureReason = reason;
        this.failedAt = Objects.requireNonNull(failedAt, "failedAt");
        this.retryable = retryable;
    }
}
