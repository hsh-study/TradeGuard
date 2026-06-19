package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.ImportDisclosureActualEvidenceUseCase;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DisclosureActualControllerTest {
    @Test void exposesImportHistoryAndEvidenceEndpoints() throws Exception {
        ImportDisclosureActualEvidenceUseCase useCase=mock(ImportDisclosureActualEvidenceUseCase.class);
        when(useCase.importWatchlist(any())).thenReturn(List.of());when(useCase.findHistories(any())).thenReturn(List.of());
        when(useCase.findEvidences(any(),any(),any())).thenReturn(List.of());
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new DisclosureActualController(useCase)).build();
        mvc.perform(post("/api/research/disclosures/import-watchlist").param("baseDate","2026-06-15")).andExpect(status().isOk());
        mvc.perform(get("/api/research/disclosures/import-histories").param("stockCode","005930")).andExpect(status().isOk());
        mvc.perform(get("/api/research/disclosures/evidences").param("stockCode","005930")
                .param("from","2026-06-01").param("to","2026-06-15")).andExpect(status().isOk());
        verify(useCase).importWatchlist(LocalDate.of(2026,6,15));
    }
}
