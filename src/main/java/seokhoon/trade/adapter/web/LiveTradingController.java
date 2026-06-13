package seokhoon.trade.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.domain.order.*;
import seokhoon.trade.domain.position.LivePosition;

import java.math.BigDecimal;
import java.util.List;

import static seokhoon.trade.application.port.in.LiveTradingUseCases.*;

@RestController
public class LiveTradingController {
    private final RequestLiveBuyUseCase buy;
    private final RequestLiveSellUseCase sell;
    private final PreviewLivePositionExitUseCase preview;
    private final LoadLiveTradingUseCase load;
    private final SetLiveTradingKillSwitchUseCase killSwitch;
    private final CancelLiveOrderUseCase cancel;

    public LiveTradingController(RequestLiveBuyUseCase buy,RequestLiveSellUseCase sell,
            PreviewLivePositionExitUseCase preview,LoadLiveTradingUseCase load,
            SetLiveTradingKillSwitchUseCase killSwitch,
            CancelLiveOrderUseCase cancel){
        this.buy=buy;this.sell=sell;this.preview=preview;this.load=load;
        this.killSwitch=killSwitch;this.cancel=cancel;
    }

    @PostMapping("/api/live-orders/buy")
    LiveOrderRequest buy(@Valid @RequestBody BuyRequest r){
        return buy.buy(r.signalId(),r.stockCode(),r.quantity(),r.orderPrice(),r.orderType());
    }

    @PostMapping("/api/live-orders/sell")
    LiveSellResult sell(@Valid @RequestBody SellRequest r){
        return sell.sell(r.positionId(),r.stockCode(),r.quantity(),r.orderPrice(),r.reason());
    }

    @GetMapping("/api/live-orders/{id}") LiveOrderRequest order(@PathVariable long id){return load.order(id);}
    @GetMapping("/api/live-orders") List<LiveOrderRequest> orders(@RequestParam(required=false) LiveOrderStatus status){return load.orders(status);}
    @GetMapping("/api/live-orders/{id}/histories") List<LiveOrderStatusHistory> histories(@PathVariable long id){return load.histories(id);}
    @GetMapping("/api/live-orders/open") List<LiveOrderRequest> openOrders(){return load.openOrders();}
    @GetMapping("/api/live-orders/{id}/fills") List<LiveTradeFill> fills(@PathVariable long id){return load.fills(id);}
    @GetMapping("/api/live-orders/{id}/cancel-requests") List<LiveOrderCancelRequest> cancelRequests(@PathVariable long id){return load.cancelRequests(id);}
    @PostMapping("/api/live-orders/{id}/cancel")
    LiveOrderCancelResult cancel(@PathVariable long id,
            @Valid @RequestBody CancelRequest request) {
        return cancel.cancel(id,request.cancelQuantity(),request.reason());
    }
    @GetMapping("/api/live-positions") List<LivePosition> positions(){return load.positions();}
    @GetMapping("/api/live-positions/{id}") LivePosition position(@PathVariable long id){return load.position(id);}
    @GetMapping("/api/live-positions/{id}/exit-preview")
    LivePositionExitPreview preview(@PathVariable long id,@RequestParam @DecimalMin("0.01") BigDecimal currentPrice){return preview.preview(id,currentPrice);}

    @PostMapping("/api/live-trading/kill-switch")
    LiveTradingRuntimeState killSwitch(@Valid @RequestBody KillSwitchRequest r){
        return killSwitch.set(r.enabled(),r.reason());
    }

    public record BuyRequest(Long signalId,@NotBlank String stockCode,
            @Min(1) int quantity,@NotNull @DecimalMin("0.01") BigDecimal orderPrice,
            @NotNull OrderType orderType){}
    public record SellRequest(Long positionId,String stockCode,@Min(1) int quantity,
            @NotNull @DecimalMin("0.01") BigDecimal orderPrice,@NotBlank String reason){}
    public record KillSwitchRequest(boolean enabled,@NotBlank String reason){}
    public record CancelRequest(@NotBlank String reason,
            @Min(1) Integer cancelQuantity){}
}
