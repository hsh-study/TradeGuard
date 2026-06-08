package seokhoon.trade.application.port.in;

import java.time.LocalDate;
import java.util.List;

public record ClosingBetCandidateScanResult(
        LocalDate tradeDate,
        int scannedCount,
        int filteredCount,
        int selectedCount,
        boolean briefingSent,
        String summary,
        List<ClosingBetPreScanCandidate> selectedCandidates
) {
}
