package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import seokhoon.trade.domain.order.*;
import seokhoon.trade.domain.position.LivePositionStatus;
import seokhoon.trade.domain.kis.KisEnvironment;

import java.util.List;
import java.util.Optional;

interface LiveOrderRequestJpaRepository extends JpaRepository<LiveOrderRequestEntity,Long> {
    boolean existsBySignalIdAndSide(Long signalId, OrderSide side);
    List<LiveOrderRequestEntity> findByStatusOrderByRequestedAtDesc(LiveOrderStatus status);
    List<LiveOrderRequestEntity> findByStatusInOrderByRequestedAtAsc(List<LiveOrderStatus> statuses);
}

interface LiveOrderCancelRequestJpaRepository
        extends JpaRepository<LiveOrderCancelRequestEntity,Long> {
    List<LiveOrderCancelRequestEntity>
            findByOrderIdOrderByRequestedAtDesc(long orderId);
}
interface LivePositionJpaRepository extends JpaRepository<LivePositionEntity,Long> {
    List<LivePositionEntity> findByStatusOrderByOpenedAtAsc(LivePositionStatus status);
    Optional<LivePositionEntity> findFirstByStockCodeAndStatusNotOrderByOpenedAtDesc(String stockCode, LivePositionStatus status);
    Optional<LivePositionEntity> findFirstByStockCodeAndEnvironmentAndStatusNotOrderByOpenedAtDesc(
            String stockCode, KisEnvironment environment, LivePositionStatus status);
}
interface LivePositionExitRuleJpaRepository extends JpaRepository<LivePositionExitRuleEntity,Long> {
    Optional<LivePositionExitRuleEntity> findByPositionId(long positionId);
}
interface LiveTradeFillJpaRepository extends JpaRepository<LiveTradeFillEntity,Long> {
    List<LiveTradeFillEntity> findByOrderIdOrderByFilledAtAsc(long orderId);
}
interface LiveOrderStatusHistoryJpaRepository extends JpaRepository<LiveOrderStatusHistoryEntity,Long> {
    List<LiveOrderStatusHistoryEntity> findByOrderIdOrderByCreatedAtAsc(long orderId);
}
interface LiveTradingRuntimeStateJpaRepository extends JpaRepository<LiveTradingRuntimeStateEntity,Long> {}
