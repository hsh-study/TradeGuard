package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import seokhoon.trade.application.port.in.GetOperationalDashboardUseCase;
import seokhoon.trade.application.port.in.GetBootReadinessReportUseCase;
import seokhoon.trade.application.port.in.OperationalDashboardSummary;

import java.time.LocalDate;

@Controller
public class OperationalDashboardPageController {
    private final GetOperationalDashboardUseCase useCase;
    private final GetBootReadinessReportUseCase bootReadiness;
    private final OperationalDashboardHtmlRenderer renderer;

    public OperationalDashboardPageController(GetOperationalDashboardUseCase useCase,
            GetBootReadinessReportUseCase bootReadiness) {
        this.useCase = useCase;
        this.bootReadiness = bootReadiness;
        this.renderer = new OperationalDashboardHtmlRenderer();
    }

    @GetMapping(value = "/operations/dashboard", produces = "text/html;charset=UTF-8")
    @ResponseBody
    String dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam(defaultValue = "overview") String view) {
        OperationalDashboardSummary summary = baseDate == null
                ? useCase.getTodayDashboard()
                : useCase.getDashboard(baseDate);
        return renderer.render(summary, bootReadiness.getLatestReport(), view);
    }
}
