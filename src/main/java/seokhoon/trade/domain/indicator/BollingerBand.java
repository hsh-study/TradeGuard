package seokhoon.trade.domain.indicator;

import java.math.BigDecimal;

public record BollingerBand(BigDecimal upper, BigDecimal middle, BigDecimal lower) {
}
