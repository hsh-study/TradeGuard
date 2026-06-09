package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import seokhoon.trade.application.port.in.ScanClosingBetCandidatesUseCase;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class ClosingBetCandidateScanScheduler {
    private static final Logger log = LoggerFactory.getLogger(ClosingBetCandidateScanScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LIMIT = 5;

    private final ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase;
    private final MarketCalendarPort marketCalendarPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final Clock clock;

    @Autowired
    public ClosingBetCandidateScanScheduler(
            ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort
    ) {
        this(
                scanClosingBetCandidatesUseCase,
                marketCalendarPort,
                historyPort,
                Clock.system(SEOUL)
        );
    }

    ClosingBetCandidateScanScheduler(
            ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            Clock clock
    ) {
        this(
                scanClosingBetCandidatesUseCase,
                marketCalendarPort,
                SchedulerExecutionHistoryPort.noop(),
                clock
        );
    }

    ClosingBetCandidateScanScheduler(
            ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            Clock clock
    ) {
        this.scanClosingBetCandidatesUseCase = scanClosingBetCandidatesUseCase;
        this.marketCalendarPort = marketCalendarPort;
        this.historyPort = historyPort;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 14 * * MON-FRI", zone = "Asia/Seoul")
    public void scanAtMarketAfternoon() {
        LocalDate tradeDate = LocalDate.now(clock);
        if (!marketCalendarPort.isTradingDay(tradeDate)) {
            historyPort.markSkipped(
                    SchedulerName.CLOSING_BET_PRE_SCAN_14,
                    tradeDate,
                    "NON_TRADING_DAY",
                    Instant.now(clock)
            );
            log.info("Skipping 14:00 closing bet candidate scan on non-trading day: {}", tradeDate);
            return;
        }
        long historyId = historyPort.saveStarted(
                SchedulerName.CLOSING_BET_PRE_SCAN_14,
                tradeDate,
                Instant.now(clock)
        );
        try {
            var result = scanClosingBetCandidatesUseCase.scan(tradeDate, DEFAULT_LIMIT);
            historyPort.markSucceeded(
                    historyId,
                    result.scannedCount(),
                    result.selectedCount(),
                    result.briefingSent(),
                    Instant.now(clock)
            );
        } catch (RuntimeException exception) {
            historyPort.markFailed(
                    historyId,
                    failureReason(exception),
                    Instant.now(clock)
            );
            throw exception;
        }
    }

    private static String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        String reason = message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getClass().getSimpleName() + ": " + message;
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
    }
}
