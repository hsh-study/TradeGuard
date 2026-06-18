package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.InvestorFlowDiagnosticPort;
import seokhoon.trade.application.port.out.KisConfigurationPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.config.InvestorFlowProperties;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.market.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class VerifyInvestorFlowProviderServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);

    @Test
    void blocksWhenDiagnosticIsDisabled() {
        InvestorFlowDiagnosticPort port = mock(InvestorFlowDiagnosticPort.class);
        InvestorFlowProperties properties = enabledProperties();
        properties.setDiagnosticEnabled(false);

        assertThatThrownBy(() -> service(port, properties).verifyStock("005930", DATE))
                .isInstanceOf(InvestorFlowDiagnosticBlockedException.class)
                .hasMessageContaining("disabled");
        verifyNoInteractions(port);
    }

    @Test
    void blocksWhenHttpIsNotAllowed() {
        InvestorFlowDiagnosticPort port = mock(InvestorFlowDiagnosticPort.class);
        InvestorFlowProperties properties = enabledProperties();
        properties.setDiagnosticAllowHttp(false);

        assertThatThrownBy(() -> service(port, properties).verifyStock("005930", DATE))
                .isInstanceOf(InvestorFlowDiagnosticBlockedException.class)
                .hasMessageContaining("not allowed");
        verifyNoInteractions(port);
    }

    @Test
    void permitsOnlyDiagnosticWhileAmountUnitIsUnverified() {
        InvestorFlowDiagnosticPort port = mock(InvestorFlowDiagnosticPort.class);
        when(port.diagnoseStock("005930", DATE)).thenReturn(data());
        InvestorFlowProperties properties = enabledProperties();

        InvestorFlowVerification result = service(port, properties)
                .verifyStock("005930", DATE);

        assertThat(result.amountUnitStatus())
                .isEqualTo(InvestorFlowAmountUnitStatus.UNVERIFIED);
        assertThat(result.warningMessages()).contains("AMOUNT_UNIT_UNVERIFIED");
        assertThat(result.rawAmountFieldsMasked())
                .containsEntry("frgn_ntby_tr_pbmn", "POSITIVE_DIGITS_3");
    }

    @Test
    void reportsVerifiedAfterExplicitAmountUnitConfiguration() {
        InvestorFlowDiagnosticPort port = mock(InvestorFlowDiagnosticPort.class);
        when(port.diagnoseMarket(InvestorFlowMarket.KOSPI, DATE)).thenReturn(data());
        InvestorFlowProperties properties = enabledProperties();
        properties.setKisAmountUnit(KisInvestorFlowAmountUnit.THOUSAND_KRW);

        InvestorFlowVerification result = service(port, properties)
                .verifyMarket(InvestorFlowMarket.KOSPI, DATE);

        assertThat(result.amountUnitStatus())
                .isEqualTo(InvestorFlowAmountUnitStatus.VERIFIED);
        assertThat(result.warningMessages()).doesNotContain("AMOUNT_UNIT_UNVERIFIED");
    }

    @Test
    void hasNoPersistenceOrderOrBrokerDependency() {
        assertThat(Arrays.stream(VerifyInvestorFlowProviderService.class.getDeclaredConstructors())
                .flatMap(value -> Arrays.stream(value.getParameterTypes()))
                .map(Class::getName))
                .noneMatch(value -> value.contains("Persistence")
                        || value.contains("InvestorFlowPort")
                        || value.contains("Order") || value.contains("Broker"));
    }

    private static VerifyInvestorFlowProviderService service(
            InvestorFlowDiagnosticPort port, InvestorFlowProperties properties) {
        KisConfigurationPort kis = mock(KisConfigurationPort.class);
        when(kis.readOnlyEnvironment()).thenReturn(KisEnvironment.DEMO);
        return new VerifyInvestorFlowProviderService(port, properties, kis,
                OperationalMetricsPort.noop());
    }

    private static InvestorFlowProperties enabledProperties() {
        InvestorFlowProperties properties = new InvestorFlowProperties();
        properties.setProviderEnabled(true);
        properties.setDiagnosticEnabled(true);
        properties.setDiagnosticAllowHttp(true);
        return properties;
    }

    private static InvestorFlowDiagnosticData data() {
        return new InvestorFlowDiagnosticData(
                "/safe-endpoint", "SAFE_TR", 1,
                List.of("stck_bsop_date", "frgn_ntby_tr_pbmn"),
                List.of("FOREIGN"),
                Map.of("frgn_ntby_tr_pbmn", "POSITIVE_DIGITS_3"),
                Map.of("frgn_ntby_qty", "POSITIVE_DIGITS_2"), true
        );
    }
}
