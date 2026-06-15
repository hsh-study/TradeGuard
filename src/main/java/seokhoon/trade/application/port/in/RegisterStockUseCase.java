package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.indicator.IndicatorWarmUpResult;

public interface RegisterStockUseCase {
    IndicatorWarmUpResult register(
            String stockCode,
            String stockName,
            Market market
    );
}
