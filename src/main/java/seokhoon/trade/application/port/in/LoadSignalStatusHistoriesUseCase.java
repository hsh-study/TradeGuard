package seokhoon.trade.application.port.in;

import java.util.List;

public interface LoadSignalStatusHistoriesUseCase {
    List<SignalStatusHistoryView> load(long signalId);
}
