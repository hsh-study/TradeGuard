package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.GetBootReadinessReportUseCase;
import seokhoon.trade.domain.operations.BootReadinessReport;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BootReadinessControllerTest {
    @Test void returns503BeforeReportGeneration() throws Exception {
        GetBootReadinessReportUseCase useCase=mock(GetBootReadinessReportUseCase.class);
        when(useCase.getLatestReport()).thenReturn(Optional.empty());
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new BootReadinessController(useCase)).build();

        mvc.perform(get("/api/operations/boot-readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("NOT_READY_TO_REPORT"));
    }

    @Test void returnsLatestReportWithoutSensitiveValues() throws Exception {
        GetBootReadinessReportUseCase useCase=mock(GetBootReadinessReportUseCase.class);
        when(useCase.getLatestReport()).thenReturn(Optional.of(report()));
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new BootReadinessController(useCase)).build();

        mvc.perform(get("/api/operations/boot-readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("READY"))
                .andExpect(content().string(not(containsString("secret-token-value"))))
                .andExpect(content().string(not(containsString("app-secret-value"))))
                .andExpect(content().string(not(containsString("123-45-67890"))))
                .andExpect(content().string(not(containsString("https://discord.example/secret"))))
                .andExpect(content().string(not(containsString("receipt-secret"))))
                .andExpect(content().string(not(containsString("external-provider-secret"))));
    }

    private static BootReadinessReport report() {
        var ready=new BootReadinessReport.ComponentStatus("READY",List.of());
        return new BootReadinessReport(Instant.parse("2026-06-20T00:00:00Z"),"test","TEST",
                BootReadinessReport.OverallStatus.READY,"1.0",ready,ready,ready,ready,ready,ready,
                ready,ready,ready,ready,ready,List.of(),List.of(),List.of());
    }
}
