package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.order.*;
import seokhoon.trade.domain.position.LivePosition;

import java.math.BigDecimal;
import java.util.List;

public final class LiveTradingUseCases {
    private LiveTradingUseCases() {}

    public interface RequestLiveBuyUseCase {
        LiveOrderRequest buy(Long signalId, String stockCode, int quantity,
                BigDecimal orderPrice, OrderType orderType);
    }
    public interface RequestLiveSellUseCase {
        LiveSellResult sell(Long positionId, String stockCode, int quantity,
                BigDecimal orderPrice, String reason);
    }
    public interface EvaluateLivePositionExitUseCase {
        List<LivePositionExitEvaluation> evaluate();
    }
    public interface PreviewLivePositionExitUseCase {
        LivePositionExitPreview preview(long positionId, BigDecimal currentPrice);
    }
    public interface LoadLiveTradingUseCase {
        LiveOrderRequest order(long id);
        List<LiveOrderRequest> orders(LiveOrderStatus status);
        List<LivePosition> positions();
        LivePosition position(long id);
        List<LiveOrderStatusHistory> histories(long orderId);
        List<LiveOrderRequest> openOrders();
        List<LiveTradeFill> fills(long orderId);
        List<LiveOrderCancelRequest> cancelRequests(long orderId);
    }
    public interface CancelLiveOrderUseCase {
        LiveOrderCancelResult cancel(long orderId, Integer cancelQuantity,
                String reason);
    }
    public interface SetLiveTradingKillSwitchUseCase {
        LiveTradingRuntimeState set(boolean enabled, String reason);
    }
    public interface ApplyLiveTradeFillUseCase {
        LivePosition apply(LiveTradeFill fill);
    }
    public interface ReconcileLiveOrdersUseCase {
        int reconcile();
    }

    public record LiveSellResult(
            LiveOrderRequest order,
            LivePositionExitPreview preview
    ) {}
    public record LivePositionExitEvaluation(
            long positionId,
            LiveExitAction action,
            LiveOrderRequest order,
            String failureReason
    ) {}
    public record LiveOrderCancelResult(
            LiveOrderRequest order,
            LiveOrderCancelRequest cancelRequest
    ) {}
}
