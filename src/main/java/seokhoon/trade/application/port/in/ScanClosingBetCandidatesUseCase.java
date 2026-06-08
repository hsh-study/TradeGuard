package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface ScanClosingBetCandidatesUseCase {
    ClosingBetCandidateScanResult scan(LocalDate tradeDate, int limit);
}
