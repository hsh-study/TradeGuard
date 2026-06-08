package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.ScanClosingBetCandidatesUseCase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class ClosingBetCandidateScanScheduler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LIMIT = 5;

    private final ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase;
    private final Clock clock;

    @Autowired
    public ClosingBetCandidateScanScheduler(ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase) {
        this(scanClosingBetCandidatesUseCase, Clock.system(SEOUL));
    }

    ClosingBetCandidateScanScheduler(
            ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase,
            Clock clock
    ) {
        this.scanClosingBetCandidatesUseCase = scanClosingBetCandidatesUseCase;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 14 * * MON-FRI", zone = "Asia/Seoul")
    public void scanAtMarketAfternoon() {
        // TODO: Skip Korean market holidays.
        scanClosingBetCandidatesUseCase.scan(LocalDate.now(clock), DEFAULT_LIMIT);
    }
}
