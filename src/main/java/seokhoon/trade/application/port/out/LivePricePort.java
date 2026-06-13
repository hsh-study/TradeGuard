package seokhoon.trade.application.port.out;

import java.math.BigDecimal;

public interface LivePricePort {
    BigDecimal getCurrentPrice(String stockCode);
}
