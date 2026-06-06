package seokhoon.trade.domain.indicator;

import java.math.BigDecimal;

public record Macd(BigDecimal macd, BigDecimal signal, BigDecimal histogram) {
}
