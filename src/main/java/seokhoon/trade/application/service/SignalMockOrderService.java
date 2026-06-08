package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.in.RequestMockOrderUseCase;
import seokhoon.trade.application.port.in.RequestSignalMockOrderUseCase;
import seokhoon.trade.application.port.in.SignalMockOrderCommand;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.util.Objects;

@Service
public class SignalMockOrderService implements RequestSignalMockOrderUseCase {
    private final TradingSignalPort tradingSignalPort;
    private final RequestMockOrderUseCase requestMockOrderUseCase;

    public SignalMockOrderService(
            TradingSignalPort tradingSignalPort,
            RequestMockOrderUseCase requestMockOrderUseCase
    ) {
        this.tradingSignalPort = tradingSignalPort;
        this.requestMockOrderUseCase = requestMockOrderUseCase;
    }

    @Override
    public MockOrderResult request(long signalId, SignalMockOrderCommand command) {
        Objects.requireNonNull(command, "command");
        TradingSignal signal = tradingSignalPort.findById(signalId)
                .orElseThrow(TradingSignalNotFoundException::new);
        return requestMockOrderUseCase.request(signal, command.quantity(), command.limitPrice());
    }
}
