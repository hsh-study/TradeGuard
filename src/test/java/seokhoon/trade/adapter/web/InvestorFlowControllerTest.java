package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.AnalyzeSupplyDemandUseCase;
import seokhoon.trade.application.port.in.ImportInvestorFlowsUseCase;
import seokhoon.trade.application.port.in.VerifyInvestorFlowProviderUseCase;
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

    private static MockMvc mvc(VerifyInvestorFlowProviderUseCase verification) {
        return MockMvcBuilders.standaloneSetup(new InvestorFlowController(
                        mock(ImportInvestorFlowsUseCase.class),
                        mock(AnalyzeSupplyDemandUseCase.class),
                        verification,
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
