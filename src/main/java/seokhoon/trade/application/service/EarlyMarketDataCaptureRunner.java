package seokhoon.trade.application.service;

import org.slf4j.Logger;
import seokhoon.trade.application.port.in.EarlyMarketDataCaptureResult;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.LocalDate;
import java.util.function.Supplier;

final class EarlyMarketDataCaptureRunner {
    private EarlyMarketDataCaptureRunner() {
    }

    static void run(
            Logger log,
            SchedulerName schedulerName,
            LocalDate tradeDate,
            Supplier<EarlyMarketDataCaptureResult> capture
    ) {
        try {
            EarlyMarketDataCaptureResult result = capture.get();
            if (result.hasFailure()) {
                log.atWarn()
                        .addKeyValue("schedulerName", schedulerName)
                        .addKeyValue("tradeDate", tradeDate)
                        .addKeyValue("capturedItemCount", result.itemCount())
                        .addKeyValue("captureStatuses", result.captures()
                                .stream()
                                .map(item -> item.captureType() + "=" + item.status())
                                .toList())
                        .log("Early market raw data capture was incomplete");
            }
        } catch (RuntimeException exception) {
            log.atWarn()
                    .addKeyValue("schedulerName", schedulerName)
                    .addKeyValue("tradeDate", tradeDate)
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("Early market raw data capture failed; strategy continues");
        }
    }
}
