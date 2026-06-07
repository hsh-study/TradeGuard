package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.in.RequestMockOrderUseCase;
import seokhoon.trade.application.port.in.RequestStoredMockOrderUseCase;
import seokhoon.trade.application.port.in.StoredMockOrderCommand;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.util.Objects;

@Service
public class StoredMockOrderService implements RequestStoredMockOrderUseCase {
    private final TradingSignalPort tradingSignalPort;
    private final RequestMockOrderUseCase requestMockOrderUseCase;

    public StoredMockOrderService(
            TradingSignalPort tradingSignalPort,
            RequestMockOrderUseCase requestMockOrderUseCase
    ) {
        this.tradingSignalPort = tradingSignalPort;
        this.requestMockOrderUseCase = requestMockOrderUseCase;
    }

    @Override
    public MockOrderResult request(StoredMockOrderCommand command) {
        validate(command);
        TradingSignal signal = tradingSignalPort.find(
                        command.strategyName(),
                        command.stockCode(),
                        command.signalDate(),
                        command.signalType()
                )
                .orElseThrow(TradingSignalNotFoundException::new);
        return requestMockOrderUseCase.request(signal, command.quantity(), command.limitPrice());
    }

    private static void validate(StoredMockOrderCommand command) {
        Objects.requireNonNull(command, "command");
        if (command.strategyName() == null || command.strategyName().isBlank()) {
            throw new IllegalArgumentException("strategyName must not be blank");
        }
        if (command.stockCode() == null || command.stockCode().isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        Objects.requireNonNull(command.signalDate(), "signalDate");
        Objects.requireNonNull(command.signalType(), "signalType");
    }
}
