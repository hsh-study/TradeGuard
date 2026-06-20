package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import seokhoon.trade.application.port.in.GetOperationalDashboardUseCase;
import seokhoon.trade.application.port.in.OperationalDashboardSummary;

import java.time.LocalDate;

@Controller
public class OperationalDashboardPageController {
    private final GetOperationalDashboardUseCase useCase;
    private final OperationalDashboardHtmlRenderer renderer;

    public OperationalDashboardPageController(GetOperationalDashboardUseCase useCase) {
        this.useCase = useCase;
        this.renderer = new OperationalDashboardHtmlRenderer();
    }

    @GetMapping(value = "/operations/dashboard", produces = "text/html;charset=UTF-8")
    @ResponseBody
    String dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        OperationalDashboardSummary summary = baseDate == null
                ? useCase.getTodayDashboard()
                : useCase.getDashboard(baseDate);
        return renderer.render(summary);
    }
}
