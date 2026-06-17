package seokhoon.trade.domain.research;

import java.util.List;
import java.util.Objects;

public record DartFinancialStatement(
        String corpCode,
        int fiscalYear,
        String reportCode,
        List<DartFinancialAccount> accounts
) {
    public DartFinancialStatement {
        Objects.requireNonNull(corpCode, "corpCode");
        Objects.requireNonNull(reportCode, "reportCode");
        accounts = List.copyOf(Objects.requireNonNull(accounts, "accounts"));
    }
}
