package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.LiveTradingProperties;
import seokhoon.trade.domain.order.*;
import seokhoon.trade.domain.position.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static seokhoon.trade.application.port.in.LiveTradingUseCases.*;

@Service
public class LiveTradingService implements RequestLiveBuyUseCase,
        RequestLiveSellUseCase, EvaluateLivePositionExitUseCase,
        PreviewLivePositionExitUseCase, LoadLiveTradingUseCase,
        SetLiveTradingKillSwitchUseCase, ApplyLiveTradeFillUseCase,
        ReconcileLiveOrdersUseCase, CancelLiveOrderUseCase {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalTime OPEN = LocalTime.of(9, 0);
    private static final LocalTime CLOSE = LocalTime.of(15, 30);

    private final LiveTradingProperties properties;
    private final LiveOrderRequestPort orders;
    private final LivePositionPort positions;
    private final LivePositionExitRulePort rules;
    private final LiveTradeFillPort fills;
    private final LiveOrderStatusHistoryPort histories;
    private final LiveOrderCancelRequestPort cancellations;
    private final LiveTradingRuntimeStatePort runtime;
    private final LiveTradingOrderPort broker;
    private final LivePricePort prices;
    private final MarketCalendarPort calendar;
    private final OperationalMetricsPort metrics;
    private final TradingAccountManagementUseCase accounts;
    private final Clock clock;

    @Autowired
    public LiveTradingService(LiveTradingProperties properties,
            LiveOrderRequestPort orders, LivePositionPort positions,
            LivePositionExitRulePort rules, LiveTradeFillPort fills,
            LiveOrderStatusHistoryPort histories,
            LiveOrderCancelRequestPort cancellations,
            LiveTradingRuntimeStatePort runtime, LiveTradingOrderPort broker,
            LivePricePort prices, MarketCalendarPort calendar,
            OperationalMetricsPort metrics,
            TradingAccountManagementUseCase accounts) {
        this(properties,orders,positions,rules,fills,histories,cancellations,runtime,broker,
                prices,calendar,metrics,accounts,Clock.system(SEOUL));
    }

    LiveTradingService(LiveTradingProperties properties,
            LiveOrderRequestPort orders, LivePositionPort positions,
            LivePositionExitRulePort rules, LiveTradeFillPort fills,
            LiveOrderStatusHistoryPort histories,
            LiveOrderCancelRequestPort cancellations,
            LiveTradingRuntimeStatePort runtime, LiveTradingOrderPort broker,
            LivePricePort prices, MarketCalendarPort calendar,
            OperationalMetricsPort metrics, Clock clock) {
        this(properties,orders,positions,rules,fills,histories,cancellations,runtime,broker,
                prices,calendar,metrics,null,clock);
    }

    LiveTradingService(LiveTradingProperties properties,
            LiveOrderRequestPort orders, LivePositionPort positions,
            LivePositionExitRulePort rules, LiveTradeFillPort fills,
            LiveOrderStatusHistoryPort histories,
            LiveOrderCancelRequestPort cancellations,
            LiveTradingRuntimeStatePort runtime, LiveTradingOrderPort broker,
            LivePricePort prices, MarketCalendarPort calendar,
            OperationalMetricsPort metrics,
            TradingAccountManagementUseCase accounts, Clock clock) {
        this.properties=properties;this.orders=orders;this.positions=positions;
        this.rules=rules;this.fills=fills;this.histories=histories;
        this.cancellations=cancellations;
        this.runtime=runtime;this.broker=broker;this.prices=prices;
        this.calendar=calendar;this.metrics=metrics;this.clock=clock;
        this.accounts=accounts;
    }

    @Override @Transactional
    public LiveOrderRequest buy(Long signalId, String stockCode, int quantity,
            BigDecimal orderPrice, OrderType orderType) {
        guardNewOrder();
        validateMarketHours();
        validateOrder(stockCode, quantity, orderPrice, orderType);
        if (signalId != null && orders.existsBySignalIdAndSide(signalId, OrderSide.BUY)) {
            throw new LiveTradingException("A live BUY order already exists for signalId");
        }
        return submit(newOrder(signalId, stockCode, OrderSide.BUY, quantity, orderPrice),
                "MANUAL_BUY");
    }

    @Override @Transactional
    public LiveSellResult sell(Long positionId, String stockCode, int quantity,
            BigDecimal orderPrice, String reason) {
        guardNewOrder();
        validateMarketHours();
        LivePosition position = resolvePosition(positionId, stockCode);
        if (position.status() != LivePositionStatus.OPEN) {
            throw new LiveTradingException("Position is not OPEN");
        }
        if (quantity <= 0 || quantity > position.quantity()) {
            throw new LiveTradingException("Sell quantity exceeds the open position");
        }
        validateOrder(position.stockCode(), quantity, orderPrice, OrderType.LIMIT);
        LivePositionExitPreview preview = preview(position.id(), orderPrice);
        LiveOrderRequest order = submit(newOrder(null, position.stockCode(),
                OrderSide.SELL, quantity, orderPrice), safeReason(reason));
        if (order.status() == LiveOrderStatus.ACCEPTED) {
            positions.updatePosition(position.withStatus(
                    LivePositionStatus.SELL_ORDERED, clock.instant()));
        }
        return new LiveSellResult(order, preview);
    }

    @Override
    public List<LivePositionExitEvaluation> evaluate() {
        guardNewOrder();
        if (!isMarketOpen()) {
            return List.of();
        }
        List<LivePositionExitEvaluation> results = new ArrayList<>();
        for (LivePosition position : positions.findOpenPositions()) {
            try {
                BigDecimal price = prices.getCurrentPrice(position.stockCode());
                LivePositionExitPreview preview = preview(position.id(), price);
                if (preview.suggestedAction() == LiveExitAction.HOLD) {
                    metrics.recordLivePositionExitEvaluation("hold");
                    results.add(new LivePositionExitEvaluation(
                            position.id(), LiveExitAction.HOLD, null, null));
                    continue;
                }
                String result = preview.maxLossTriggered() ? "max_loss"
                        : preview.takeProfitTriggered() ? "take_profit" : "stop_loss";
                metrics.recordLivePositionExitEvaluation(result);
                LiveSellResult sell = sell(position.id(), null, position.quantity(),
                        price, preview.suggestedAction().name());
                results.add(new LivePositionExitEvaluation(position.id(),
                        preview.suggestedAction(), sell.order(), null));
            } catch (RuntimeException exception) {
                metrics.recordLivePositionExitEvaluation("failure");
                results.add(new LivePositionExitEvaluation(position.id(),
                        LiveExitAction.HOLD, null, exception.getClass().getSimpleName()));
            }
        }
        return List.copyOf(results);
    }

    @Override
    public LivePositionExitPreview preview(long positionId, BigDecimal currentPrice) {
        LivePosition position = position(positionId);
        LivePositionExitRule rule = rules.findByPositionId(positionId)
                .orElseGet(() -> defaultRule(position));
        return calculate(position, rule, currentPrice);
    }

    @Override public LiveOrderRequest order(long id){return orders.findOrderById(id).orElseThrow(()->new LiveTradingException("Live order not found"));}
    @Override public List<LiveOrderRequest> orders(LiveOrderStatus status){return status==null?orders.findAll():orders.findByStatus(status);}
    @Override public List<LivePosition> positions(){return positions.findOpenPositions();}
    @Override public LivePosition position(long id){return positions.findPositionById(id).orElseThrow(()->new LiveTradingException("Live position not found"));}
    @Override public List<LiveOrderStatusHistory> histories(long id){order(id);return histories.findHistoriesByOrderId(id);}
    @Override public List<LiveOrderRequest> openOrders(){return orders.findOpenSubmittedOrders();}
    @Override public List<LiveTradeFill> fills(long id){order(id);return fills.findFillsByOrderId(id);}
    @Override public List<LiveOrderCancelRequest> cancelRequests(long id){order(id);return cancellations.findByOrderId(id);}

    @Override @Transactional
    public LiveTradingRuntimeState set(boolean enabled, String reason) {
        return runtime.save(new LiveTradingRuntimeState(
                enabled, safeReason(reason), clock.instant()));
    }

    @Override @Transactional
    public LivePosition apply(LiveTradeFill fill) {
        LiveOrderRequest order = order(fill.liveOrderRequestId());
        List<LiveTradeFill> existingFills = fills.findFillsByOrderId(order.id());
        int previouslyFilled = existingFills.stream()
                .mapToInt(LiveTradeFill::filledQuantity).sum();
        if (previouslyFilled + fill.filledQuantity() > order.quantity()) {
            throw new LiveTradingException("Fill quantity exceeds order quantity");
        }
        fills.save(fill);
        LiveOrderStatus from = order.status();
        int totalFilled = previouslyFilled + fill.filledQuantity();
        int remaining = order.quantity() - totalFilled;
        LiveOrderStatus fillStatus = remaining > 0
                ? LiveOrderStatus.PARTIALLY_FILLED
                : LiveOrderStatus.FILLED;
        LiveOrderRequest filled = orders.save(order.withExecution(
                totalFilled, remaining, fillStatus, clock.instant()));
        history(filled.id(), from, fillStatus, "KIS_FILL_CONFIRMED");
        return applyFillToPosition(fill, fillStatus);
    }

    private LivePosition applyFillToPosition(LiveTradeFill fill,
            LiveOrderStatus fillStatus) {
        if (fill.side() == OrderSide.BUY) {
            seokhoon.trade.domain.kis.KisEnvironment environment = tradingEnvironment();
            LivePosition position = findPosition(fill.stockCode(), environment)
                    .filter(existing -> existing.status() == LivePositionStatus.OPEN)
                    .map(existing -> mergeBuyFill(existing, fill))
                    .orElseGet(() -> positions.savePosition(new LivePosition(
                            null, fill.stockCode(), environment, fill.filledQuantity(),
                            fill.filledPrice(), fill.filledAmount(), fill.fee(),
                            LivePositionStatus.OPEN, fill.filledAt(), null)));
            rules.save(defaultRule(position));
            return position;
        }
        LivePosition position = positions.findByStockCode(fill.stockCode())
                .orElseThrow(() -> new LiveTradingException("Open position not found"));
        if (fill.filledQuantity() < position.quantity()) {
            int remainingQuantity = position.quantity() - fill.filledQuantity();
            BigDecimal remainingRatio = BigDecimal.valueOf(remainingQuantity)
                    .divide(BigDecimal.valueOf(position.quantity()), 8,
                            RoundingMode.HALF_UP);
            return positions.updatePosition(new LivePosition(
                    position.id(), position.stockCode(), position.environment(),
                    remainingQuantity,
                    position.averageBuyPrice(),
                    position.buyAmount().multiply(remainingRatio),
                    position.buyCommission().multiply(remainingRatio),
                    fillStatus == LiveOrderStatus.PARTIALLY_FILLED
                            ? LivePositionStatus.SELL_ORDERED
                            : LivePositionStatus.OPEN,
                    position.openedAt(), null));
        }
        return positions.updatePosition(position.withStatus(
                LivePositionStatus.CLOSED, fill.filledAt()));
    }

    @Override
    public int reconcile() {
        properties.validateKisAccessEnabled();
        List<LiveOrderRequest> openOrders = orders.findOpenSubmittedOrders();
        if (openOrders.isEmpty()) return 0;
        int updated = 0;
        for (LiveOpenOrderSnapshot snapshot :
                broker.inquireOpenOrders(openOrders)) {
            try {
                LiveOrderRequest current = order(snapshot.liveOrderRequestId());
                int cumulative = Math.min(current.quantity(),
                        Math.max(0, snapshot.filledQuantity()));
                int delta = cumulative - current.filledQuantity();
                if (delta > 0 && snapshot.averageFilledPrice() != null) {
                    BigDecimal cumulativeAmount = snapshot.averageFilledPrice()
                            .multiply(BigDecimal.valueOf(cumulative));
                    BigDecimal previousAmount = fills.findFillsByOrderId(
                            current.id()).stream()
                            .map(LiveTradeFill::filledAmount)
                            .reduce(BigDecimal.ZERO,BigDecimal::add);
                    BigDecimal amount = cumulativeAmount.subtract(
                            previousAmount);
                    BigDecimal deltaPrice = amount.divide(
                            BigDecimal.valueOf(delta),4,RoundingMode.HALF_UP);
                    BigDecimal fee = money(amount.multiply(
                            current.side() == OrderSide.BUY
                                    ? properties.getBuyCommissionRate()
                                    : properties.getSellCommissionRate()));
                    BigDecimal tax = current.side() == OrderSide.SELL
                            ? money(amount.multiply(properties.getSellTaxRate()))
                            : BigDecimal.ZERO;
                    LiveTradeFill fill = new LiveTradeFill(null,current.id(),
                            current.stockCode(),current.side(),delta,
                            deltaPrice,amount,fee,tax,
                            snapshot.inquiredAt());
                    fills.save(fill);
                    LiveOrderStatus fillStatus = cumulative == current.quantity()
                            ? LiveOrderStatus.FILLED
                            : LiveOrderStatus.PARTIALLY_FILLED;
                    applyFillToPosition(fill, fillStatus);
                }
                int remaining = Math.max(0,
                        Math.min(current.quantity() - cumulative,
                                snapshot.remainingQuantity()));
                LiveOrderStatus next = remaining == 0
                        ? cumulative == current.quantity()
                                ? LiveOrderStatus.FILLED
                                : LiveOrderStatus.CANCELED
                        : cumulative > 0 ? LiveOrderStatus.PARTIALLY_FILLED
                        : LiveOrderStatus.ACCEPTED;
                LiveOrderRequest saved = orders.save(current.withExecution(
                        cumulative, remaining, next, snapshot.inquiredAt()));
                if (current.status() != next) {
                    history(saved.id(),current.status(),next,
                            "KIS_ORDER_RECONCILED");
                }
                metrics.recordLiveOrderReconciliation(
                        next == LiveOrderStatus.FILLED ? "filled"
                                : next == LiveOrderStatus.PARTIALLY_FILLED
                                ? "partial" : "updated");
                updated++;
                if (shouldAutoCancel(saved)) {
                    cancel(saved.id(), null, "AUTO_CANCEL_POLICY");
                }
            } catch (RuntimeException exception) {
                metrics.recordLiveOrderReconciliation("failure");
            }
        }
        return updated;
    }

    @Override
    @Transactional(noRollbackFor = LiveTradingException.class)
    public LiveOrderCancelResult cancel(long orderId, Integer cancelQuantity,
            String reason) {
        properties.validateKisAccessEnabled();
        LiveOrderRequest order = order(orderId);
        if (order.status() != LiveOrderStatus.ACCEPTED
                && order.status() != LiveOrderStatus.PARTIALLY_FILLED) {
            throw new LiveTradingException(
                    "Only ACCEPTED or PARTIALLY_FILLED orders can be canceled");
        }
        int remaining = order.remainingQuantity() > 0
                ? order.remainingQuantity()
                : order.quantity() - order.filledQuantity();
        int quantity = cancelQuantity == null ? remaining : cancelQuantity;
        if (quantity <= 0 || quantity > remaining) {
            throw new LiveTradingException(
                    "cancelQuantity exceeds remaining quantity");
        }
        Instant now = clock.instant();
        LiveOrderCancelRequest request = cancellations.save(
                new LiveOrderCancelRequest(null,order.id(),
                        order.kisOriginalOrderNo(),quantity,
                        LiveOrderCancelStatus.CREATED,null,null,
                        safeReason(reason),now,null,now));
        LiveOrderRequest requested = orders.save(order.withCancelRequested(now));
        history(order.id(),order.status(),LiveOrderStatus.CANCEL_REQUESTED,
                request.reason());
        try {
            request = cancellations.save(request.withResult(
                    LiveOrderCancelStatus.SUBMITTED,null,null,now));
            LiveOrderCancellation response = broker.cancelOrder(order,quantity,
                    quantity == remaining);
            if (!response.accepted()) {
                cancellations.save(request.withResult(
                        LiveOrderCancelStatus.REJECTED,null,
                        sanitize(response.failureReason()),clock.instant()));
                orders.save(requested.withStatus(order.status(),
                        order.kisOrderNo(),order.kisOriginalOrderNo(),
                        null,clock.instant()));
                history(order.id(),LiveOrderStatus.CANCEL_REQUESTED,
                        order.status(),"KIS_CANCEL_REJECTED");
                metrics.recordLiveOrderCancel("failure");
                throw new LiveTradingException("KIS cancel order was rejected");
            }
            LiveOrderCancelRequest accepted = cancellations.save(
                    request.withResult(LiveOrderCancelStatus.ACCEPTED,
                            response.cancelOrderNo(),null,clock.instant()));
            LiveOrderRequest canceled = orders.save(
                    requested.withCancellationAccepted(quantity,
                            clock.instant()));
            history(order.id(),LiveOrderStatus.CANCEL_REQUESTED,
                    canceled.status(),"KIS_CANCEL_ACCEPTED");
            if (canceled.status() == LiveOrderStatus.CANCELED) {
                reopenSellPositionAfterCancel(order);
            }
            metrics.recordLiveOrderCancel("success");
            return new LiveOrderCancelResult(canceled,accepted);
        } catch (LiveTradingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            cancellations.save(request.withResult(LiveOrderCancelStatus.FAILED,
                    null,exception.getClass().getSimpleName(),clock.instant()));
            orders.save(requested.withStatus(order.status(),order.kisOrderNo(),
                    order.kisOriginalOrderNo(),null,clock.instant()));
            history(order.id(),LiveOrderStatus.CANCEL_REQUESTED,order.status(),
                    "KIS_CANCEL_FAILED");
            metrics.recordLiveOrderCancel("failure");
            throw new LiveTradingException("KIS cancel order failed");
        }
    }

    private LiveOrderRequest submit(LiveOrderRequest created, String reason) {
        LiveOrderRequest saved = orders.save(created);
        history(saved.id(), null, LiveOrderStatus.CREATED, reason);
        LiveOrderRequest approved = orders.save(saved.withStatus(
                LiveOrderStatus.RISK_APPROVED, null, null, null, clock.instant()));
        history(approved.id(), LiveOrderStatus.CREATED,
                LiveOrderStatus.RISK_APPROVED, "LIVE_ORDER_POLICY_APPROVED");
        metrics.recordLiveOrderRequest(approved.side().name(), "RISK_APPROVED");
        try {
            LiveOrderRequest submitted = orders.save(approved.withStatus(
                    LiveOrderStatus.SUBMITTED, null, null, null, clock.instant()));
            history(submitted.id(), LiveOrderStatus.RISK_APPROVED,
                    LiveOrderStatus.SUBMITTED, "KIS_ORDER_SUBMITTING");
            LiveOrderSubmission response = submitted.side() == OrderSide.BUY
                    ? broker.submitBuyLimitOrder(submitted)
                    : broker.submitSellLimitOrder(submitted);
            if (!response.accepted()) {
                LiveOrderRequest rejected = orders.save(submitted.withStatus(
                        LiveOrderStatus.REJECTED, null, null,
                        sanitize(response.failureReason()), clock.instant()));
                history(rejected.id(), LiveOrderStatus.SUBMITTED,
                        LiveOrderStatus.REJECTED, rejected.failureReason());
                metrics.recordLiveOrderSubmit(submitted.side().name(), "failure");
                return rejected;
            }
            LiveOrderRequest accepted = orders.save(submitted.withStatus(
                    LiveOrderStatus.ACCEPTED, response.orderNo(),
                    response.originalOrderNo(), null, clock.instant()));
            int expireMinutes = accepted.side() == OrderSide.BUY
                    ? properties.getBuyOrderExpireMinutes()
                    : properties.getSellOrderExpireMinutes();
            accepted = orders.save(accepted.withExpireAt(
                    clock.instant().plus(Duration.ofMinutes(expireMinutes)),
                    clock.instant()));
            history(accepted.id(), LiveOrderStatus.SUBMITTED,
                    LiveOrderStatus.ACCEPTED, "KIS_ORDER_ACCEPTED");
            metrics.recordLiveOrderSubmit(submitted.side().name(), "success");
            return accepted;
        } catch (RuntimeException exception) {
            LiveOrderRequest failed = orders.save(approved.withStatus(
                    LiveOrderStatus.FAILED, null, null,
                    exception.getClass().getSimpleName(), clock.instant()));
            history(failed.id(), LiveOrderStatus.RISK_APPROVED,
                    LiveOrderStatus.FAILED, failed.failureReason());
            metrics.recordLiveOrderSubmit(approved.side().name(), "failure");
            return failed;
        }
    }

    private LiveOrderRequest newOrder(Long signalId, String stockCode,
            OrderSide side, int quantity, BigDecimal price) {
        Instant now=clock.instant();
        return new LiveOrderRequest(null,signalId,stockCode.trim(),side,quantity,
                price,OrderType.LIMIT,LiveOrderStatus.CREATED,null,null,null,
                now,null,now,quantity,0,null,null,null,null);
    }

    private void guardNewOrder() {
        properties.validateOrderEnabled();
        if (runtime.get().killSwitchEnabled()) {
            throw new LiveTradingException("Live trading kill switch is enabled");
        }
    }

    private void validateOrder(String stockCode,int quantity,BigDecimal price,OrderType type) {
        if (stockCode==null||stockCode.isBlank()) throw new LiveTradingException("stockCode is required");
        if (quantity<=0) throw new LiveTradingException("quantity must be positive");
        if (price==null||price.signum()<=0) throw new LiveTradingException("orderPrice must be positive");
        if (type!=OrderType.LIMIT) throw new LiveTradingException("Only LIMIT orders are allowed");
        if (price.multiply(BigDecimal.valueOf(quantity)).compareTo(properties.getMaxAllowedOrderAmount())>0)
            throw new LiveTradingException("Order amount exceeds maxAllowedOrderAmount");
    }

    private void validateMarketHours() {
        if (!isMarketOpen()) throw new LiveTradingException("Live orders are allowed only during market hours");
    }

    private boolean isMarketOpen() {
        ZonedDateTime now=ZonedDateTime.now(clock).withZoneSameInstant(SEOUL);
        return calendar.isTradingDay(now.toLocalDate())
                && !now.toLocalTime().isBefore(OPEN)
                && !now.toLocalTime().isAfter(CLOSE);
    }

    private LivePosition resolvePosition(Long id,String code) {
        if(id!=null)return position(id);
        if(code==null||code.isBlank())throw new LiveTradingException("positionId or stockCode is required");
        return findPosition(code, tradingEnvironment())
                .orElseThrow(()->new LiveTradingException("Open position not found"));
    }

    private LivePositionExitRule defaultRule(LivePosition position) {
        BigDecimal take=properties.getDefaultTakeProfitRate();
        BigDecimal stop=properties.getDefaultStopLossRate();
        if(position.averageBuyPrice().compareTo(properties.getHighPriceThreshold2())>=0){
            take=new BigDecimal("3.0");stop=new BigDecimal("-2.0");
        }else if(position.averageBuyPrice().compareTo(properties.getHighPriceThreshold1())>=0){
            take=new BigDecimal("4.0");stop=new BigDecimal("-2.5");
        }
        Instant now=clock.instant();
        return new LivePositionExitRule(null,position.id(),take,stop,
                properties.getMaxLossAmountPerPosition(),properties.getSellTaxRate(),
                properties.getBuyCommissionRate(),properties.getSellCommissionRate(),
                true,now,now);
    }

    private LivePosition mergeBuyFill(
            LivePosition existing,
            LiveTradeFill fill
    ) {
        int quantity = existing.quantity() + fill.filledQuantity();
        BigDecimal amount = existing.buyAmount().add(fill.filledAmount());
        BigDecimal average = amount.divide(
                BigDecimal.valueOf(quantity),
                4,
                RoundingMode.HALF_UP
        );
        return positions.updatePosition(new LivePosition(
                existing.id(), existing.stockCode(), existing.environment(), quantity, average, amount,
                existing.buyCommission().add(fill.fee()),
                LivePositionStatus.OPEN, existing.openedAt(), null
        ));
    }

    private seokhoon.trade.domain.kis.KisEnvironment tradingEnvironment() {
        if (accounts != null) {
            return accounts.primaryCredentials()
                    .map(TradingAccountManagementUseCase.AccountCredentials::environment)
                    .orElseGet(properties::environment);
        }
        return properties.environment();
    }

    private java.util.Optional<LivePosition> findPosition(String stockCode,
            seokhoon.trade.domain.kis.KisEnvironment environment) {
        java.util.Optional<LivePosition> exact =
                positions.findByStockCodeAndEnvironment(stockCode, environment);
        if (exact.isPresent()) return exact;
        return positions.findByStockCode(stockCode)
                .filter(position -> position.environment() == null);
    }

    private boolean shouldAutoCancel(LiveOrderRequest order) {
        if (!properties.isLiveOrderAutoCancelEnabled()
                || order.remainingQuantity() <= 0) return false;
        Instant now = clock.instant();
        if (order.expireAt() != null && !now.isBefore(order.expireAt())) {
            return true;
        }
        ZonedDateTime seoul = ZonedDateTime.now(clock)
                .withZoneSameInstant(SEOUL);
        return calendar.isTradingDay(seoul.toLocalDate())
                && !seoul.toLocalTime().isBefore(CLOSE.minusMinutes(
                        properties.getCancelBeforeMarketCloseMinutes()));
    }

    private void reopenSellPositionAfterCancel(LiveOrderRequest order) {
        if (order.side() != OrderSide.SELL) return;
        positions.findByStockCode(order.stockCode())
                .filter(position -> position.status()
                        == LivePositionStatus.SELL_ORDERED)
                .ifPresent(position -> positions.updatePosition(
                        position.withStatus(LivePositionStatus.OPEN,
                                clock.instant())));
    }

    private LivePositionExitPreview calculate(LivePosition p,
            LivePositionExitRule r,BigDecimal currentPrice) {
        if(currentPrice==null||currentPrice.signum()<=0)throw new LiveTradingException("currentPrice must be positive");
        BigDecimal buy=p.buyAmount();
        BigDecimal gross=currentPrice.multiply(BigDecimal.valueOf(p.quantity()));
        BigDecimal buyFee=p.buyCommission().signum()>0?p.buyCommission():money(buy.multiply(r.buyCommissionRate()));
        BigDecimal sellFee=money(gross.multiply(r.sellCommissionRate()));
        BigDecimal tax=money(gross.multiply(r.sellTaxRate()));
        BigDecimal net=gross.subtract(tax).subtract(sellFee).subtract(buy).subtract(buyFee);
        BigDecimal rate=net.multiply(BigDecimal.valueOf(100)).divide(buy,4,RoundingMode.HALF_UP);
        boolean maxLoss=net.compareTo(r.maxLossAmount().negate())<=0;
        boolean take=rate.compareTo(r.takeProfitRate())>=0
                && net.compareTo(properties.getMinimumNetProfitAmount())>=0;
        boolean stop=rate.compareTo(r.stopLossRate())<=0;
        LiveExitAction action=maxLoss||stop?LiveExitAction.SELL_STOP_LOSS:
                take?LiveExitAction.SELL_TAKE_PROFIT:LiveExitAction.HOLD;
        return new LivePositionExitPreview(buy,gross,tax,buyFee,sellFee,net,
                rate,take,stop,maxLoss,action);
    }

    private static BigDecimal money(BigDecimal value){return value.setScale(0,RoundingMode.HALF_UP);}
    private void history(long id,LiveOrderStatus from,LiveOrderStatus to,String reason){histories.save(new LiveOrderStatusHistory(null,id,from,to,sanitize(reason),clock.instant()));}
    private static String safeReason(String reason){return reason==null||reason.isBlank()?"MANUAL_API":sanitize(reason);}
    private static String sanitize(String value){if(value==null)return null;String clean=value.replaceAll("[\\r\\n\\t]"," ");return clean.length()>1000?clean.substring(0,1000):clean;}
}
