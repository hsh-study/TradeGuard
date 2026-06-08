package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public record ClosingBetBriefingResult(
        boolean sent,
        int candidateCount,
        int riskCandidateCount,
        String summary,
        String messageBody,
        LocalDate signalDate
) {
}
