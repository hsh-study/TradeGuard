package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.GetOperationalDashboardUseCase;
import seokhoon.trade.application.port.in.OperationalDashboardSummary;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/operations/dashboard")
public class OperationalDashboardController {
    private final GetOperationalDashboardUseCase useCase;

    public OperationalDashboardController(GetOperationalDashboardUseCase useCase) { this.useCase = useCase; }

    @GetMapping
    OperationalDashboardSummary dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return baseDate == null ? useCase.getTodayDashboard() : useCase.getDashboard(baseDate);
    }
}
