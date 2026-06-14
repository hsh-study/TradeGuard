package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.KisTokenUseCases.*;
import seokhoon.trade.domain.kis.KisEnvironment;

import java.time.*;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class KisTokenControllerTest {
    @Test
    void exposesOnlyTokenMetadataAndManualRefresh() throws Exception {
        var useCase=mock(ManageKisTokenUseCase.class);
        KisTokenStatus status=new KisTokenStatus(KisEnvironment.DEMO,true,
                Instant.parse("2026-06-14T00:00:00Z"),3600,
                LocalDate.of(2026,6,13));
        when(useCase.statuses()).thenReturn(List.of(status));
        when(useCase.refresh(KisEnvironment.DEMO)).thenReturn(status);
        MockMvc mvc=MockMvcBuilders.standaloneSetup(
                new KisTokenController(useCase)).build();

        mvc.perform(get("/api/kis/token/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].environment").value("DEMO"))
                .andExpect(jsonPath("$[0].tokenPresent").value(true))
                .andExpect(jsonPath("$[0].accessToken").doesNotExist());
        mvc.perform(post("/api/kis/token/refresh")
                        .param("environment","DEMO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environment").value("DEMO"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }
}
