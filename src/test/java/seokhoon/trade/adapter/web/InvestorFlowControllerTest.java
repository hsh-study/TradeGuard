package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.AnalyzeSupplyDemandUseCase;
import seokhoon.trade.application.port.in.ImportInvestorFlowsUseCase;
import seokhoon.trade.application.port.in.VerifyInvestorFlowProviderUseCase;
import seokhoon.trade.application.port.in.GetInvestorFlowReadinessUseCase;
import seokhoon.trade.application.port.in.InvestorFlowReadiness;
import seokhoon.trade.application.port.out.MarketInvestorFlowPort;
import seokhoon.trade.application.port.out.StockInvestorFlowPort;
import seokhoon.trade.application.port.out.SupplyDemandSnapshotPort;
import seokhoon.trade.application.service.InvestorFlowDiagnosticBlockedException;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.market.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvestorFlowControllerTest {
    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);

    @Test
    void exposesSafeStockDiagnosticResponse() throws Exception {
        VerifyInvestorFlowProviderUseCase verification =
                mock(VerifyInvestorFlowProviderUseCase.class);
        when(verification.verifyStock("005930", DATE)).thenReturn(verification());
        MockMvc mvc = mvc(verification);

        MvcResult result = mvc.perform(post("/api/research/investor-flows/verify/stock")
                        .param("stockCode", "005930")
                        .param("tradeDate", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("KIS"))
                .andExpect(jsonPath("$.trId").value("FHKST01010900"))
                .andExpect(jsonPath("$.amountUnitStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.rawAmountFieldsMasked.frgn_ntby_tr_pbmn")
                        .value("POSITIVE_DIGITS_3"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("token", "authorization", "header", "appKey",
                        "appSecret", "account", "rawBody");
    }

    @Test
    void returnsConflictWhenDiagnosticGateBlocksHttp() throws Exception {
        VerifyInvestorFlowProviderUseCase verification =
                mock(VerifyInvestorFlowProviderUseCase.class);
        when(verification.verifyMarket(InvestorFlowMarket.KOSPI, DATE))
                .thenThrow(new InvestorFlowDiagnosticBlockedException(
                        "KIS investor flow diagnostic HTTP is not allowed"));

        mvc(verification).perform(post("/api/research/investor-flows/verify/market")
                        .param("market", "KOSPI")
                        .param("tradeDate", "2026-06-15"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("INVESTOR_FLOW_DIAGNOSTIC_BLOCKED"));
    }

    @Test
    void exposesReadinessWithoutCredentialsOrAccountData() throws Exception {
        GetInvestorFlowReadinessUseCase readiness = mock(GetInvestorFlowReadinessUseCase.class);
        when(readiness.getReadiness()).thenReturn(new InvestorFlowReadiness(
                true, "KIS", KisInvestorFlowAmountUnit.UNVERIFIED, false,
                true, true, true, false, 20,
                null, null, null, false,
                List.of("AMOUNT_UNIT_UNVERIFIED"), List.of(),
                List.of("Run verify stock API")));

        MvcResult result = mvc(mock(VerifyInvestorFlowProviderUseCase.class), readiness)
                .perform(get("/api/research/investor-flows/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.amountUnit").value("UNVERIFIED"))
                .andExpect(jsonPath("$.blockingReasons[0]")
                        .value("AMOUNT_UNIT_UNVERIFIED"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("token", "appKey", "appSecret", "account", "header");
    }

    private static MockMvc mvc(VerifyInvestorFlowProviderUseCase verification) {
        return mvc(verification, mock(GetInvestorFlowReadinessUseCase.class));
    }

    private static MockMvc mvc(VerifyInvestorFlowProviderUseCase verification,
            GetInvestorFlowReadinessUseCase readiness) {
        return MockMvcBuilders.standaloneSetup(new InvestorFlowController(
                        mock(ImportInvestorFlowsUseCase.class),
                        mock(AnalyzeSupplyDemandUseCase.class),
                        verification,
                        readiness,
                        mock(StockInvestorFlowPort.class),
                        mock(MarketInvestorFlowPort.class),
                        mock(SupplyDemandSnapshotPort.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static InvestorFlowVerification verification() {
        return new InvestorFlowVerification(
                InvestorFlowProvider.KIS,
                "/uapi/domestic-stock/v1/quotations/inquire-investor",
                "FHKST01010900",
                KisEnvironment.DEMO,
                DATE,
                1,
                List.of("stck_bsop_date", "frgn_ntby_tr_pbmn"),
                List.of("FOREIGN"),
                Map.of("frgn_ntby_tr_pbmn", "POSITIVE_DIGITS_3"),
                Map.of("frgn_ntby_qty", "POSITIVE_DIGITS_2"),
                InvestorFlowAmountUnitStatus.UNVERIFIED,
                List.of("AMOUNT_UNIT_UNVERIFIED", "SAMPLE_VALUES_MASKED"),
                "Verify the unit"
        );
    }
}
