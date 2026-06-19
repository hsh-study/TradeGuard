package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.GetOperationalDashboardUseCase;

import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperationalDashboardControllerTest {
    @Test void delegatesExplicitBaseDate() throws Exception {
        GetOperationalDashboardUseCase useCase = mock(GetOperationalDashboardUseCase.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new OperationalDashboardController(useCase)).build();

        mvc.perform(get("/api/operations/dashboard").param("baseDate", "2026-06-15"))
                .andExpect(status().isOk());

        verify(useCase).getDashboard(LocalDate.of(2026, 6, 15));
    }
}
