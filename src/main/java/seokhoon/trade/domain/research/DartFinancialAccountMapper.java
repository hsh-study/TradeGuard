package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public final class DartFinancialAccountMapper {
    private static final List<String> REVENUE = List.of("매출액", "수익(매출액)");
    private static final List<String> OPERATING_INCOME = List.of("영업이익");
    private static final List<String> NET_INCOME = List.of("당기순이익", "당기순이익(손실)");
    private static final List<String> TOTAL_ASSETS = List.of("자산총계");
    private static final List<String> TOTAL_LIABILITIES = List.of("부채총계");
    private static final List<String> TOTAL_EQUITY = List.of("자본총계");
    private static final List<String> OPERATING_CASH_FLOW = List.of("영업활동현금흐름", "영업활동으로 인한 현금흐름");

    private DartFinancialAccountMapper() {
    }

    public static Optional<BigDecimal> revenue(List<DartFinancialAccount> accounts) {
        return find(accounts, REVENUE);
    }

    public static Optional<BigDecimal> operatingIncome(List<DartFinancialAccount> accounts) {
        return find(accounts, OPERATING_INCOME);
    }

    public static Optional<BigDecimal> netIncome(List<DartFinancialAccount> accounts) {
        return find(accounts, NET_INCOME);
    }

    public static Optional<BigDecimal> totalAssets(List<DartFinancialAccount> accounts) {
        return find(accounts, TOTAL_ASSETS);
    }

    public static Optional<BigDecimal> totalLiabilities(List<DartFinancialAccount> accounts) {
        return find(accounts, TOTAL_LIABILITIES);
    }

    public static Optional<BigDecimal> totalEquity(List<DartFinancialAccount> accounts) {
        return find(accounts, TOTAL_EQUITY);
    }

    public static Optional<BigDecimal> operatingCashFlow(List<DartFinancialAccount> accounts) {
        return find(accounts, OPERATING_CASH_FLOW);
    }

    private static Optional<BigDecimal> find(List<DartFinancialAccount> accounts, List<String> candidates) {
        for (String candidate : candidates) {
            Optional<BigDecimal> exact = accounts.stream()
                    .filter(account -> account.accountName().equals(candidate))
                    .map(DartFinancialAccount::amount)
                    .filter(amount -> amount != null)
                    .findFirst();
            if (exact.isPresent()) {
                return exact;
            }
        }
        List<String> normalizedCandidates = candidates.stream()
                .map(DartFinancialAccountMapper::normalize)
                .toList();
        return accounts.stream()
                .filter(account -> normalizedCandidates.contains(normalize(account.accountName())))
                .map(DartFinancialAccount::amount)
                .filter(amount -> amount != null)
                .findFirst();
    }

    static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s()（）]", "");
    }
}
