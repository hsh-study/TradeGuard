package seokhoon.trade.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.AccountSelectedTradingUseCase;
import seokhoon.trade.application.port.in.LiveTradingUseCases.LiveSellResult;
import seokhoon.trade.domain.order.LiveOrderRequest;
import seokhoon.trade.domain.order.OrderType;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/operations/orders")
public class AccountSelectedTradingController {
    private final AccountSelectedTradingUseCase useCase;

    public AccountSelectedTradingController(AccountSelectedTradingUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/buy")
    LiveOrderRequest buy(@Valid @RequestBody BuyRequest request) {
        return useCase.buy(new AccountSelectedTradingUseCase.BuyCommand(
                request.accountId(), request.signalId(), request.stockCode(),
                request.quantity(), request.orderPrice(), request.orderType(),
                request.realTradingConfirmed()));
    }

    @PostMapping("/sell")
    LiveSellResult sell(@Valid @RequestBody SellRequest request) {
        return useCase.sell(new AccountSelectedTradingUseCase.SellCommand(
                request.accountId(), request.positionId(), request.stockCode(),
                request.quantity(), request.orderPrice(), request.reason(),
                request.realTradingConfirmed()));
    }

    public record BuyRequest(@Min(1) long accountId, Long signalId,
            @NotBlank String stockCode, @Min(1) int quantity,
            @NotNull @DecimalMin("0.01") BigDecimal orderPrice,
            @NotNull OrderType orderType, boolean realTradingConfirmed) {}

    public record SellRequest(@Min(1) long accountId,
            @NotNull Long positionId, String stockCode, @Min(1) int quantity,
            @NotNull @DecimalMin("0.01") BigDecimal orderPrice,
            @NotBlank String reason, boolean realTradingConfirmed) {}
}
