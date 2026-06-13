package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.order.LiveTradingRuntimeState;

import java.time.Instant;

@Entity
@Table(name="live_trading_runtime_state")
class LiveTradingRuntimeStateEntity {
    @Id Long id;
    @Column(name="kill_switch_enabled",nullable=false) boolean enabled;
    @Column String reason;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    protected LiveTradingRuntimeStateEntity(){}
    LiveTradingRuntimeState toDomain(){return new LiveTradingRuntimeState(enabled,reason,updatedAt);}
    void update(LiveTradingRuntimeState v){enabled=v.killSwitchEnabled();reason=v.reason();updatedAt=v.updatedAt();}
}
