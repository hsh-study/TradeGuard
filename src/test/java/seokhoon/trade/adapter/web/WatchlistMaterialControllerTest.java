package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.WatchlistMaterialUseCase;
import seokhoon.trade.domain.research.DisclosureEvidenceImportStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class WatchlistMaterialControllerTest {
    @Test
    void collectsDefaultThirtyDaysAndDoesNotExposeFailureDetail() throws Exception {
        WatchlistMaterialUseCase useCase = mock(WatchlistMaterialUseCase.class);
        LocalDate to = LocalDate.of(2026, 6, 20), from = to.minusDays(30);
        when(useCase.collect("005930", from, to)).thenReturn(
                new WatchlistMaterialUseCase.CollectionResult("005930", from, to,
                        DisclosureEvidenceImportStatus.SKIPPED, 0,
                        "DISCLOSURE_PROVIDER_DISABLED_OR_NOT_READY"));
        var controller = new WatchlistMaterialController(useCase,
                Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC));

        standaloneSetup(controller).build()
                .perform(post("/api/stocks/005930/materials/collect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"))
                .andExpect(jsonPath("$.message").value("DISCLOSURE_PROVIDER_DISABLED_OR_NOT_READY"))
                .andExpect(content().string(not(containsString("failureReason"))));
        verify(useCase).collect("005930", from, to);
    }

    @Test
    void returnsSafeMaterialList() throws Exception {
        WatchlistMaterialUseCase useCase = mock(WatchlistMaterialUseCase.class);
        when(useCase.find(anyString(), any(), any())).thenReturn(List.of());

        standaloneSetup(new WatchlistMaterialController(useCase)).build()
                .perform(get("/api/stocks/005930/materials")
                        .param("from", "2026-06-01").param("to", "2026-06-20"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andExpect(content().string(not(containsString("sourceUrl"))))
                .andExpect(content().string(not(containsString("receiptNo"))));
    }
}
