package seokhoon.trade.application.port.in;

import java.time.LocalDate;
import java.util.List;

public record EarlyMarketScanResult(
        LocalDate tradeDate,
        int scannedCount,
        int selectedCount,
        boolean briefingSent,
        String summary,
        List<EarlyMarketCandidate> selectedCandidates
) {
}
