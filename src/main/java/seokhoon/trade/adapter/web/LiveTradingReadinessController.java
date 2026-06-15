package seokhoon.trade.adapter.web;

import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.LiveTradingReadinessUseCase;
import seokhoon.trade.domain.order.LiveTradingReadinessReport;

@RestController
@RequestMapping("/api/live-trading")
public class LiveTradingReadinessController {
    private final LiveTradingReadinessUseCase useCase;

    public LiveTradingReadinessController(
            LiveTradingReadinessUseCase useCase
    ) {
        this.useCase=useCase;
    }

    @GetMapping("/readiness")
    LiveTradingReadinessReport readiness() {
        return useCase.checkReadiness();
    }
}
