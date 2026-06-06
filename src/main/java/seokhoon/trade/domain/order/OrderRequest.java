package seokhoon.trade.domain.order;

import java.math.BigDecimal;
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
    public String strategyName() { return strategyName; }
    public LocalDate tradeDate() { return tradeDate; }

    public void markRequested() { this.status = OrderStatus.REQUESTED; }
    public void accept(String brokerOrderNo) {
        this.status = OrderStatus.ACCEPTED;
        this.brokerOrderNo = brokerOrderNo;
    }
    public void reject() { this.status = OrderStatus.REJECTED; }
}
