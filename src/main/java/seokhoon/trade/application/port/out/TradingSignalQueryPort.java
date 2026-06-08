package seokhoon.trade.application.port.out;

import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;

import java.util.List;

public interface TradingSignalQueryPort {
    List<TradingSignalRecord> find(TradingSignalSearchCriteria criteria);
}
