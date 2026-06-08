package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import seokhoon.trade.application.port.in.ScanClosingBetCandidatesUseCase;
import seokhoon.trade.application.port.out.MarketCalendarPort;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class ClosingBetCandidateScanScheduler {
    private static final Logger log = LoggerFactory.getLogger(ClosingBetCandidateScanScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LIMIT = 5;

    private final ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase;
    private final MarketCalendarPort marketCalendarPort;
    private final Clock clock;

    @Autowired
    public ClosingBetCandidateScanScheduler(
            ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort
    ) {
        this(scanClosingBetCandidatesUseCase, marketCalendarPort, Clock.system(SEOUL));
    }

    ClosingBetCandidateScanScheduler(
            ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            Clock clock
    ) {
        this.scanClosingBetCandidatesUseCase = scanClosingBetCandidatesUseCase;
        this.marketCalendarPort = marketCalendarPort;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 14 * * MON-FRI", zone = "Asia/Seoul")
    public void scanAtMarketAfternoon() {
        LocalDate tradeDate = LocalDate.now(clock);
        if (!marketCalendarPort.isTradingDay(tradeDate)) {
            log.info("Skipping 14:00 closing bet candidate scan on non-trading day: {}", tradeDate);
            return;
        }
        scanClosingBetCandidatesUseCase.scan(tradeDate, DEFAULT_LIMIT);
    }
}
