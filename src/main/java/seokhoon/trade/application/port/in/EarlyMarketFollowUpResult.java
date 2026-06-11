package seokhoon.trade.application.port.in;

import java.time.LocalDate;
import java.util.List;

public record EarlyMarketFollowUpResult(
        LocalDate tradeDate,
        int checkedCount,
        int keepCount,
        int cautionCount,
        int excludeCount,
        boolean briefingSent,
        List<EarlyMarketFollowUpCandidate> candidates
) {
}
