package seokhoon.trade.domain.research;

import java.time.LocalDate;
import java.util.List;

public record ValuationGenerationResult(
        String stockCode,
        LocalDate baseDate,
        ValuationGenerationStatus status,
        ValuationSnapshot snapshot,
        List<String> reasons
) {
    public ValuationGenerationResult {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
