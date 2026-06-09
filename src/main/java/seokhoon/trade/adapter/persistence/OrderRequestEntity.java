package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.application.port.out.OrderRequestRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "order_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_request_stock_strategy_date_side",
                columnNames = {"stock_code", "strategy_name", "trade_date", "side"}
        )
)
public class OrderRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", nullable = false)
    private String stockCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false)
    private OrderSide side;
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;
    @Column(name = "quantity", nullable = false)
    private int quantity;
    @Column(name = "limit_price", nullable = false)
    private BigDecimal limitPrice;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;
    @Column(name = "broker_order_no")
    private String brokerOrderNo;
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    @Column(name = "failed_at")
    private Instant failedAt;
    @Column(name = "retryable", nullable = false)
    private boolean retryable;
    @Column(name = "strategy_name", nullable = false)
    private String strategyName;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "signal_id")
    private Long signalId;

    protected OrderRequestEntity() {
    }

    public static OrderRequestEntity from(OrderRequest orderRequest) {
        OrderRequestEntity entity = new OrderRequestEntity();
        entity.update(orderRequest);
        return entity;
    }

    public void update(OrderRequest orderRequest) {
        stockCode = orderRequest.stockCode();
        side = orderRequest.side();
        orderType = orderRequest.orderType();
        quantity = orderRequest.quantity();
        limitPrice = orderRequest.limitPrice();
        status = orderRequest.status();
        brokerOrderNo = orderRequest.brokerOrderNo();
        failureReason = orderRequest.failureReason();
        failedAt = orderRequest.failedAt();
        retryable = orderRequest.retryable();
        strategyName = orderRequest.strategyName();
        tradeDate = orderRequest.tradeDate();
        signalId = orderRequest.signalId();
    }

    OrderStatus status() {
        return status;
    }

    Long signalId() {
        return signalId;
    }

    OrderRequest toDomain() {
        return OrderRequest.restore(
                stockCode,
                side,
                orderType,
                quantity,
                limitPrice,
                status,
                brokerOrderNo,
                failureReason,
                failedAt,
                retryable,
                strategyName,
                tradeDate,
                signalId
        );
    }

    OrderRequestRecord toRecord() {
        return new OrderRequestRecord(
                id,
                stockCode,
                side,
                orderType,
                quantity,
                limitPrice,
                status,
                brokerOrderNo,
                failureReason,
                failedAt,
                retryable,
                strategyName,
                tradeDate,
                signalId
        );
    }
}
