package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.order.*;
import seokhoon.trade.domain.position.*;

import java.util.List;
import java.util.Optional;

@Component
public class LiveTradingPersistenceAdapter implements LiveOrderRequestPort,
        LivePositionPort, LivePositionExitRulePort, LiveTradeFillPort,
        LiveOrderStatusHistoryPort, LiveTradingRuntimeStatePort {
    private final LiveOrderRequestJpaRepository orders;
    private final LivePositionJpaRepository positions;
    private final LivePositionExitRuleJpaRepository rules;
    private final LiveTradeFillJpaRepository fills;
    private final LiveOrderStatusHistoryJpaRepository histories;
    private final LiveTradingRuntimeStateJpaRepository runtime;

    public LiveTradingPersistenceAdapter(LiveOrderRequestJpaRepository orders,
            LivePositionJpaRepository positions, LivePositionExitRuleJpaRepository rules,
            LiveTradeFillJpaRepository fills, LiveOrderStatusHistoryJpaRepository histories,
            LiveTradingRuntimeStateJpaRepository runtime) {
        this.orders=orders;this.positions=positions;this.rules=rules;this.fills=fills;
        this.histories=histories;this.runtime=runtime;
    }

    @Transactional public LiveOrderRequest save(LiveOrderRequest v) {
        LiveOrderRequestEntity e=v.id()==null?LiveOrderRequestEntity.from(v):
                orders.findById(v.id()).orElseThrow();
        e.update(v); return orders.saveAndFlush(e).toDomain();
    }
    @Transactional(readOnly=true) public Optional<LiveOrderRequest> findOrderById(long id){return orders.findById(id).map(LiveOrderRequestEntity::toDomain);}
    @Transactional(readOnly=true) public List<LiveOrderRequest> findAll(){return orders.findAll().stream().map(LiveOrderRequestEntity::toDomain).toList();}
    @Transactional(readOnly=true) public List<LiveOrderRequest> findByStatus(LiveOrderStatus s){return orders.findByStatusOrderByRequestedAtDesc(s).stream().map(LiveOrderRequestEntity::toDomain).toList();}
    @Transactional(readOnly=true) public List<LiveOrderRequest> findOpenSubmittedOrders(){return orders.findByStatusInOrderByRequestedAtAsc(List.of(LiveOrderStatus.SUBMITTED,LiveOrderStatus.ACCEPTED,LiveOrderStatus.PARTIALLY_FILLED)).stream().map(LiveOrderRequestEntity::toDomain).toList();}
    @Transactional(readOnly=true) public boolean existsBySignalIdAndSide(long id,OrderSide side){return orders.existsBySignalIdAndSide(id,side);}

    @Transactional public LivePosition savePosition(LivePosition v){return positions.saveAndFlush(LivePositionEntity.from(v)).toDomain();}
    @Transactional(readOnly=true) public List<LivePosition> findOpenPositions(){return positions.findByStatusOrderByOpenedAtAsc(LivePositionStatus.OPEN).stream().map(LivePositionEntity::toDomain).toList();}
    @Transactional(readOnly=true) public Optional<LivePosition> findPositionById(long id){return positions.findById(id).map(LivePositionEntity::toDomain);}
    @Transactional(readOnly=true) public Optional<LivePosition> findByStockCode(String code){return positions.findFirstByStockCodeAndStatusNotOrderByOpenedAtDesc(code,LivePositionStatus.CLOSED).map(LivePositionEntity::toDomain);}
    @Transactional public LivePosition updatePosition(LivePosition v){var e=positions.findById(v.id()).orElseThrow();e.update(v);return positions.saveAndFlush(e).toDomain();}

    @Transactional public LivePositionExitRule save(LivePositionExitRule v){var e=rules.findByPositionId(v.positionId()).orElseGet(()->LivePositionExitRuleEntity.from(v));e.update(v);return rules.saveAndFlush(e).toDomain();}
    @Transactional(readOnly=true) public Optional<LivePositionExitRule> findByPositionId(long id){return rules.findByPositionId(id).map(LivePositionExitRuleEntity::toDomain);}
    @Transactional public LiveTradeFill save(LiveTradeFill v){return fills.saveAndFlush(LiveTradeFillEntity.from(v)).toDomain();}
    @Transactional(readOnly=true) public List<LiveTradeFill> findFillsByOrderId(long id){return fills.findByOrderIdOrderByFilledAtAsc(id).stream().map(LiveTradeFillEntity::toDomain).toList();}
    @Transactional public void save(LiveOrderStatusHistory v){histories.save(LiveOrderStatusHistoryEntity.from(v));}
    @Transactional(readOnly=true) public List<LiveOrderStatusHistory> findHistoriesByOrderId(long id){return histories.findByOrderIdOrderByCreatedAtAsc(id).stream().map(LiveOrderStatusHistoryEntity::toDomain).toList();}
    @Transactional(readOnly=true) public LiveTradingRuntimeState get(){return runtime.findById(1L).orElseThrow().toDomain();}
    @Transactional public LiveTradingRuntimeState save(LiveTradingRuntimeState v){var e=runtime.findById(1L).orElseThrow();e.update(v);return runtime.saveAndFlush(e).toDomain();}
}
