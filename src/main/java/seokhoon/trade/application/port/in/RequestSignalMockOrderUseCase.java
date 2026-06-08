package seokhoon.trade.application.port.in;

public interface RequestSignalMockOrderUseCase {
    MockOrderResult request(long signalId, SignalMockOrderCommand command);
}
