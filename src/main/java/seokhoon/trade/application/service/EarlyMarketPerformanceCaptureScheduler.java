package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.CaptureEarlyMarketPerformancesUseCase;
import seokhoon.trade.application.port.in.EarlyMarketPerformanceCaptureResult;
import seokhoon.trade.application.port.in.EarlyMarketPerformanceView;
import seokhoon.trade.application.port.out.CorrelationIdProvider;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Component
public class EarlyMarketPerformanceCaptureScheduler {
    private static final Logger log =
            LoggerFactory.getLogger(EarlyMarketPerformanceCaptureScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final CaptureEarlyMarketPerformancesUseCase captureUseCase;
    private final MarketCalendarPort marketCalendarPort;
    private final NotificationPort notificationPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final OperationalMetricsPort metricsPort;
    private final CorrelationIdProvider correlationIdProvider;
    private final Clock clock;

    @Autowired
    public EarlyMarketPerformanceCaptureScheduler(
            CaptureEarlyMarketPerformancesUseCase captureUseCase,
            MarketCalendarPort marketCalendarPort,
            NotificationPort notificationPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider
    ) {
        this(
                captureUseCase,
                marketCalendarPort,
                notificationPort,
                historyPort,
                metricsPort,
                correlationIdProvider,
                Clock.system(SEOUL)
        );
    }

    EarlyMarketPerformanceCaptureScheduler(
            CaptureEarlyMarketPerformancesUseCase captureUseCase,
            MarketCalendarPort marketCalendarPort,
            NotificationPort notificationPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider,
            Clock clock
    ) {
        this.captureUseCase = captureUseCase;
        this.marketCalendarPort = marketCalendarPort;
        this.notificationPort = notificationPort;
        this.historyPort = historyPort;
        this.metricsPort = metricsPort;
        this.correlationIdProvider = correlationIdProvider;
        this.clock = clock;
    }

    @Scheduled(cron = "0 31 9 * * MON-FRI", zone = "Asia/Seoul")
    public void captureAfterOpeningWindow() {
        String correlationId = correlationIdProvider.newCorrelationId();
        MDC.put("correlationId", correlationId);
        try {
            execute(correlationId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void execute(String correlationId) {
        SchedulerName schedulerName =
                SchedulerName.EARLY_MARKET_PERFORMANCE_CAPTURE_930;
        LocalDate tradeDate = LocalDate.now(clock);
        if (!marketCalendarPort.isTradingDay(tradeDate)) {
            historyPort.markSkipped(
                    schedulerName,
                    tradeDate,
                    "NON_TRADING_DAY",
                    correlationId,
                    Instant.now(clock)
            );
            metricsPort.recordSchedulerExecution(
                    schedulerName,
                    SchedulerExecutionStatus.SKIPPED
            );
            logResult(
                    schedulerName,
                    tradeDate,
                    SchedulerExecutionStatus.SKIPPED,
                    0,
                    0,
                    false,
                    correlationId
            );
            return;
        }

        long historyId = historyPort.saveStarted(
                schedulerName,
                tradeDate,
                correlationId,
                Instant.now(clock)
        );
        metricsPort.recordSchedulerExecution(
                schedulerName,
                SchedulerExecutionStatus.STARTED
        );
        logResult(
                schedulerName,
                tradeDate,
                SchedulerExecutionStatus.STARTED,
                0,
                0,
                false,
                correlationId
        );
        try {
            EarlyMarketPerformanceCaptureResult result =
                    captureUseCase.capture(tradeDate);
            NotificationDeliveryResult notification = sendBriefing(result);
            historyPort.markSucceeded(
                    historyId,
                    result.signalCount(),
                    result.capturedCount(),
                    notification.sent(),
                    Instant.now(clock)
            );
            metricsPort.recordSchedulerExecution(
                    schedulerName,
                    SchedulerExecutionStatus.SUCCEEDED
            );
            metricsPort.recordSchedulerSelected(
                    schedulerName,
                    result.capturedCount()
            );
            metricsPort.recordSchedulerNotification(
                    schedulerName,
                    notification.sent()
            );
            logResult(
                    schedulerName,
                    tradeDate,
                    SchedulerExecutionStatus.SUCCEEDED,
                    result.signalCount(),
                    result.capturedCount(),
                    notification.sent(),
                    correlationId
            );
        } catch (RuntimeException exception) {
            historyPort.markFailed(
                    historyId,
                    failureReason(exception),
                    Instant.now(clock)
            );
            metricsPort.recordSchedulerExecution(
                    schedulerName,
                    SchedulerExecutionStatus.FAILED
            );
            log.atError()
                    .addKeyValue("schedulerName", schedulerName)
                    .addKeyValue("tradeDate", tradeDate)
                    .addKeyValue("result", SchedulerExecutionStatus.FAILED)
                    .addKeyValue("correlationId", correlationId)
                    .setCause(exception)
                    .log("Early market performance capture scheduler failed");
            throw exception;
        }
    }

    private NotificationDeliveryResult sendBriefing(
            EarlyMarketPerformanceCaptureResult result
    ) {
        try {
            return notificationPort.send(new NotificationMessage(
                    "TradeGuard 09:30 장초반 성과 요약 - " + result.tradeDate(),
                    briefingBody(result),
                    clock.instant()
            ));
        } catch (RuntimeException exception) {
            return NotificationDeliveryResult.skipped("notification delivery failed");
        }
    }

    static String briefingBody(EarlyMarketPerformanceCaptureResult result) {
        List<EarlyMarketPerformanceView> performances = result.performances();
        long barsUsed = performances.stream()
                .filter(performance -> performance.entryReferencePrice() != null)
                .count();
        long snapshotProxy = performances.size() - barsUsed;
        long vwapBroken = performances.stream()
                .filter(performance -> Boolean.TRUE.equals(performance.vwapBroken()))
                .count();
        List<EarlyMarketPerformanceView> topReturns = performances.stream()
                .filter(performance -> performance.maxReturnRateUntil0930() != null)
                .sorted(Comparator.comparing(
                        EarlyMarketPerformanceView::maxReturnRateUntil0930,
                        Comparator.reverseOrder()
                ))
                .limit(3)
                .toList();

        StringBuilder body = new StringBuilder("tradeDate: ")
                .append(result.tradeDate())
                .append("\n09:30 장초반 후보 성과입니다. 주문은 생성하지 않습니다.\n\n")
                .append("- 후보 수: ").append(result.signalCount()).append('\n')
                .append("- 캡처 성공 수: ").append(result.capturedCount()).append('\n')
                .append("- bars_used: ").append(barsUsed).append('\n')
                .append("- snapshot_proxy: ").append(snapshotProxy).append('\n')
                .append("- vwapBroken 후보 수: ").append(vwapBroken).append('\n')
                .append("\n상위 maxReturn 후보\n");
        if (topReturns.isEmpty()) {
            return body.append("- 분봉 기반 수익률 없음\n").toString();
        }
        topReturns.forEach(performance -> body.append("- signalId=")
                .append(performance.signalId())
                .append(", stockCode=")
                .append(performance.stockCode())
                .append(", maxReturnRateUntil0930=")
                .append(formatRate(performance.maxReturnRateUntil0930()))
                .append("%\n"));
        return body.toString();
    }

    private static String formatRate(BigDecimal rate) {
        return rate.stripTrailingZeros().toPlainString();
    }

    private static void logResult(
            SchedulerName schedulerName,
            LocalDate tradeDate,
            SchedulerExecutionStatus result,
            int signalCount,
            int capturedCount,
            boolean notificationSent,
            String correlationId
    ) {
        log.atInfo()
                .addKeyValue("schedulerName", schedulerName)
                .addKeyValue("tradeDate", tradeDate)
                .addKeyValue("signalCount", signalCount)
                .addKeyValue("capturedCount", capturedCount)
                .addKeyValue("notificationSent", notificationSent)
                .addKeyValue("result", result)
                .addKeyValue("correlationId", correlationId)
                .log("Early market performance capture scheduler status");
    }

    private static String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        String reason = message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getClass().getSimpleName() + ": " + message;
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
    }
}
