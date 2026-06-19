package seokhoon.trade.domain.research;

import java.util.Locale;

public final class DisclosureActualPolicy {
    private DisclosureActualPolicy() {}

    public static CatalystType relatedCatalystType(String title) {
        String value = normalize(title);
        if (containsAny(value, "실적", "매출", "영업이익", "손익", "사업보고서", "분기보고서", "반기보고서")) {
            return CatalystType.EARNINGS;
        }
        if (containsAny(value, "수주", "공급계약", "단일판매", "판매ㆍ공급계약", "판매·공급계약")) {
            return CatalystType.ORDER_CONTRACT;
        }
        return CatalystType.DISCLOSURE;
    }

    public static CatalystImportance importance(String title) {
        String value = normalize(title);
        if (containsAny(value, "증자", "전환사채", "감자", "최대주주", "불성실공시", "임원", "횡령", "배임")) {
            return CatalystImportance.HIGH;
        }
        if (relatedCatalystType(title) == CatalystType.EARNINGS
                || relatedCatalystType(title) == CatalystType.ORDER_CONTRACT) {
            return CatalystImportance.MEDIUM;
        }
        return CatalystImportance.LOW;
    }

    public static String disclosureType(String title, String rawCategory) {
        CatalystType catalystType = relatedCatalystType(title);
        if (importance(title) == CatalystImportance.HIGH) return "HIGH_RISK_DISCLOSURE";
        if (catalystType == CatalystType.EARNINGS) return "EARNINGS";
        if (catalystType == CatalystType.ORDER_CONTRACT) return "ORDER_CONTRACT";
        return rawCategory == null || rawCategory.isBlank() ? "GENERAL" : rawCategory;
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) if (value.contains(candidate)) return true;
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
