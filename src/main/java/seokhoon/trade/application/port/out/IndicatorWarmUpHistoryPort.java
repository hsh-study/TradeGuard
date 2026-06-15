package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.indicator.IndicatorWarmUpHistory;
import seokhoon.trade.domain.indicator.IndicatorWarmUpResult;

import java.time.Instant;
import java.util.List;

public interface IndicatorWarmUpHistoryPort {
    IndicatorWarmUpHistory save(IndicatorWarmUpResult result,
                                String failureReason, Instant createdAt);

    List<IndicatorWarmUpHistory> findByStockCode(String stockCode);
}
