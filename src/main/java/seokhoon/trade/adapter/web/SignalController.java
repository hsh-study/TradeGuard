package seokhoon.trade.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.LoadTradingSignalsUseCase;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.in.RequestSignalMockOrderUseCase;
import seokhoon.trade.application.port.in.SignalMockOrderCommand;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.in.TradingSignalView;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/signals")
public class SignalController {
    private final LoadTradingSignalsUseCase loadTradingSignalsUseCase;
    private final RequestSignalMockOrderUseCase requestSignalMockOrderUseCase;

    public SignalController(
            LoadTradingSignalsUseCase loadTradingSignalsUseCase,
            RequestSignalMockOrderUseCase requestSignalMockOrderUseCase
    ) {
        this.loadTradingSignalsUseCase = loadTradingSignalsUseCase;
        this.requestSignalMockOrderUseCase = requestSignalMockOrderUseCase;
    }

    @GetMapping
    List<TradingSignalResponse> find(
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate signalDate,
            @RequestParam(required = false) String strategyName,
            @RequestParam(required = false) SignalType signalType,
            @RequestParam(required = false) TradingSignalStatus status,
            @RequestParam(required = false) Integer minScore
    ) {
        return loadTradingSignalsUseCase.load(new TradingSignalSearchCriteria(
                        stockCode,
                        signalDate,
                        strategyName,
                        signalType,
                        status,
                        minScore
                ))
                .stream()
                .map(TradingSignalResponse::from)
                .toList();
    }

    @PostMapping("/{signalId}/mock-orders")
    MockOrderController.MockOrderResponse requestMockOrder(
            @PathVariable long signalId,
            @Valid @RequestBody SignalMockOrderRequest request
    ) {
        MockOrderResult result = requestSignalMockOrderUseCase.request(
                signalId,
                new SignalMockOrderCommand(request.quantity(), request.limitPrice())
        );
        return MockOrderController.MockOrderResponse.from(result);
    }

    public record SignalMockOrderRequest(
            @Min(1) int quantity,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal limitPrice
    ) {
    }

    public record TradingSignalResponse(
            Long signalId,
            String strategyName,
            String stockCode,
            LocalDate signalDate,
            SignalType signalType,
            int score,
            List<String> reasons,
            List<String> riskReasons,
            TradingSignalStatus status
    ) {
        static TradingSignalResponse from(TradingSignalView view) {
            return new TradingSignalResponse(
                    view.signalId(),
                    view.strategyName(),
                    view.stockCode(),
                    view.signalDate(),
                    view.signalType(),
                    view.score(),
                    view.reasons(),
                    view.riskReasons(),
                    view.status()
            );
        }
    }
}
