package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "order_requests")
public class OrderRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String stockCode;
    @Enumerated(EnumType.STRING)
    private OrderSide side;
    @Enumerated(EnumType.STRING)
    private OrderType orderType;
    private int quantity;
    private BigDecimal limitPrice;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private String brokerOrderNo;
    private String strategyName;
    private LocalDate tradeDate;

    protected OrderRequestEntity() {
    }

    public static OrderRequestEntity from(OrderRequest orderRequest) {
        OrderRequestEntity entity = new OrderRequestEntity();
        entity.stockCode = orderRequest.stockCode();
        entity.side = orderRequest.side();
        entity.orderType = orderRequest.orderType();
        entity.quantity = orderRequest.quantity();
        entity.limitPrice = orderRequest.limitPrice();
        entity.status = orderRequest.status();
        entity.brokerOrderNo = orderRequest.brokerOrderNo();
        entity.strategyName = orderRequest.strategyName();
        entity.tradeDate = orderRequest.tradeDate();
        return entity;
    }
}
