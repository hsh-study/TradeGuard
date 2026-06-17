package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.util.Objects;

public record DartFinancialAccount(
        String accountName,
        BigDecimal amount
) {
    public DartFinancialAccount {
        Objects.requireNonNull(accountName, "accountName");
    }
}
