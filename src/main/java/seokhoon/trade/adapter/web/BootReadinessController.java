package seokhoon.trade.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.GetBootReadinessReportUseCase;

@RestController
@RequestMapping("/api/operations/boot-readiness")
public class BootReadinessController {
    private final GetBootReadinessReportUseCase useCase;

    public BootReadinessController(GetBootReadinessReportUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    ResponseEntity<?> report() {
        return useCase.getLatestReport()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new PendingReport("NOT_READY_TO_REPORT")));
    }

    record PendingReport(String status) {}
}
