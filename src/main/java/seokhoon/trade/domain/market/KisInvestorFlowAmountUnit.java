package seokhoon.trade.domain.market;

import java.math.BigDecimal;

public enum KisInvestorFlowAmountUnit {
    UNVERIFIED(null),
    KRW(BigDecimal.ONE),
    THOUSAND_KRW(BigDecimal.valueOf(1_000)),
    MILLION_KRW(BigDecimal.valueOf(1_000_000));

    private final BigDecimal multiplier;

    KisInvestorFlowAmountUnit(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    public BigDecimal toKrw(BigDecimal rawAmount) {
        if (multiplier == null) {
            throw new UnsupportedOperationException(
                    "KIS investor flow amount unit is unverified; configure KIS_INVESTOR_FLOW_AMOUNT_UNIT");
        }
        return rawAmount.multiply(multiplier);
    }
}
