package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.ExternalApiConfigurationUseCase;
import seokhoon.trade.domain.kis.KisEnvironment;
import java.time.Instant;import java.util.*;
import static org.hamcrest.Matchers.*;import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExternalApiConfigurationControllerTest {
    @Test void exposesOnlyMaskedKisConfiguration() throws Exception {
        var useCase=mock(ExternalApiConfigurationUseCase.class);
        when(useCase.kisConfigs()).thenReturn(List.of(new ExternalApiConfigurationUseCase.KisConfigView(
                KisEnvironment.REAL,true,true,"********1234","https://real.example",true,Instant.now())));
        MockMvcBuilders.standaloneSetup(new ExternalApiConfigurationController(useCase)).build()
                .perform(get("/api/external-api-configurations/kis"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].maskedAppKey").value("********1234"))
                .andExpect(content().string(not(containsString("real-secret"))));
    }
}
