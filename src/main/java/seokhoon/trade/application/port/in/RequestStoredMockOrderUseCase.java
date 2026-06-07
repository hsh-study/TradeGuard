package seokhoon.trade.application.port.in;

public interface RequestStoredMockOrderUseCase {
    MockOrderResult request(StoredMockOrderCommand command);
}
