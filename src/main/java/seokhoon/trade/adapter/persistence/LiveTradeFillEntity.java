package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.order.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="live_trade_fills")
class LiveTradeFillEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="live_order_request_id",nullable=false) long orderId;
    @Column(name="stock_code",nullable=false) String stockCode;
    @Enumerated(EnumType.STRING) @Column(nullable=false) OrderSide side;
    @Column(name="filled_quantity",nullable=false) int quantity;
    @Column(name="filled_price",nullable=false) BigDecimal price;
    @Column(name="filled_amount",nullable=false) BigDecimal amount;
    @Column(nullable=false) BigDecimal fee;
    @Column(nullable=false) BigDecimal tax;
    @Column(name="filled_at",nullable=false) Instant filledAt;
    protected LiveTradeFillEntity() {}
    static LiveTradeFillEntity from(LiveTradeFill v) { var e=new LiveTradeFillEntity(); e.orderId=v.liveOrderRequestId();e.stockCode=v.stockCode();e.side=v.side();e.quantity=v.filledQuantity();e.price=v.filledPrice();e.amount=v.filledAmount();e.fee=v.fee();e.tax=v.tax();e.filledAt=v.filledAt();return e; }
    LiveTradeFill toDomain(){return new LiveTradeFill(id,orderId,stockCode,side,quantity,price,amount,fee,tax,filledAt);}
}
