package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.order.*;

import java.time.Instant;

@Entity
@Table(name="live_order_status_histories")
class LiveOrderStatusHistoryEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="live_order_request_id",nullable=false) long orderId;
    @Enumerated(EnumType.STRING) @Column(name="from_status") LiveOrderStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name="to_status",nullable=false) LiveOrderStatus toStatus;
    @Column(length=1000) String reason;
    @Column(name="created_at",nullable=false) Instant createdAt;
    protected LiveOrderStatusHistoryEntity(){}
    static LiveOrderStatusHistoryEntity from(LiveOrderStatusHistory v){var e=new LiveOrderStatusHistoryEntity();e.orderId=v.liveOrderRequestId();e.fromStatus=v.fromStatus();e.toStatus=v.toStatus();e.reason=v.reason();e.createdAt=v.createdAt();return e;}
    LiveOrderStatusHistory toDomain(){return new LiveOrderStatusHistory(id,orderId,fromStatus,toStatus,reason,createdAt);}
}
