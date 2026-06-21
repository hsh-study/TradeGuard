package seokhoon.trade.domain.order;

import seokhoon.trade.domain.kis.KisEnvironment;

import java.time.Instant;

public record TradingAccount(
        Long id,
        String alias,
        KisEnvironment environment,
        String accountNumber,
        String productCode,
        boolean active,
        boolean primaryAccount,
        Instant createdAt,
        Instant updatedAt
) {
    public TradingAccount {
        if (alias == null || alias.isBlank()) throw new IllegalArgumentException("alias is required");
        if (environment == null) throw new IllegalArgumentException("environment is required");
        if (accountNumber == null || !accountNumber.matches("\\d{8}")) {
            throw new IllegalArgumentException("accountNumber must be 8 digits");
        }
        if (productCode == null || !productCode.matches("\\d{2}")) {
            throw new IllegalArgumentException("productCode must be 2 digits");
        }
    }

    public String maskedAccountNumber() {
        return "******" + accountNumber.substring(6);
    }
}
