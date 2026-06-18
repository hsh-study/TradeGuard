package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.VerifyInvestorFlowProviderUseCase;
import seokhoon.trade.application.port.out.KisConfigurationPort;
import seokhoon.trade.application.port.out.InvestorFlowDiagnosticPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.config.InvestorFlowProperties;
import seokhoon.trade.domain.market.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
public class VerifyInvestorFlowProviderService implements VerifyInvestorFlowProviderUseCase {
    private final InvestorFlowDiagnosticPort diagnosticPort;
    private final InvestorFlowProperties properties;
    private final KisConfigurationPort kisConfiguration;
    private final OperationalMetricsPort metrics;

    public VerifyInvestorFlowProviderService(InvestorFlowDiagnosticPort diagnosticPort,
            InvestorFlowProperties properties, KisConfigurationPort kisConfiguration,
            OperationalMetricsPort metrics) {
        this.diagnosticPort = diagnosticPort;
        this.properties = properties;
        this.kisConfiguration = kisConfiguration;
        this.metrics = metrics;
    }

    @Override
    public InvestorFlowVerification verifyStock(String stockCode, LocalDate tradeDate) {
        return verify("stock", tradeDate,
                () -> diagnosticPort.diagnoseStock(stockCode, tradeDate));
    }

    @Override
    public InvestorFlowVerification verifyMarket(InvestorFlowMarket market, LocalDate tradeDate) {
        return verify("market", tradeDate,
                () -> diagnosticPort.diagnoseMarket(market, tradeDate));
    }

    private InvestorFlowVerification verify(String scope, LocalDate tradeDate,
            Supplier<InvestorFlowDiagnosticData> supplier) {
        try {
            validateEnabled();
        } catch (InvestorFlowDiagnosticBlockedException exception) {
            metrics.recordInvestorFlowDiagnostic(scope, "blocked");
            throw exception;
        }
        try {
            InvestorFlowDiagnosticData data = supplier.get();
            metrics.recordInvestorFlowDiagnostic(scope, "success");
            return result(tradeDate, data);
        } catch (RuntimeException exception) {
            metrics.recordInvestorFlowDiagnostic(scope, "failure");
            throw exception;
        }
    }

    private void validateEnabled() {
        if (!properties.isDiagnosticEnabled()) {
            throw new InvestorFlowDiagnosticBlockedException(
                    "KIS investor flow diagnostic is disabled");
        }
        if (!properties.isDiagnosticAllowHttp()) {
            throw new InvestorFlowDiagnosticBlockedException(
                    "KIS investor flow diagnostic HTTP is not allowed");
        }
        if (!properties.isProviderEnabled()
                || !"KIS".equalsIgnoreCase(properties.getProviderType())) {
            throw new InvestorFlowDiagnosticBlockedException(
                    "KIS investor flow provider is not enabled");
        }
    }

    private InvestorFlowVerification result(LocalDate tradeDate,
            InvestorFlowDiagnosticData data) {
        boolean verified = properties.isKisAmountUnitVerified();
        List<String> warnings = new ArrayList<>();
        if (!verified) {
            warnings.add("AMOUNT_UNIT_UNVERIFIED");
        }
        if (!data.requestedTradeDateFound()) {
            warnings.add("REQUESTED_TRADE_DATE_NOT_FOUND");
        }
        if (properties.isDiagnosticMaskResponse()) {
            warnings.add("SAMPLE_VALUES_MASKED");
        }
        String nextAction = verified
                ? "Disable diagnostic mode and run the normal investor flow import"
                : "Compare the limited sample with an official KIS/HTS source, configure KRW, THOUSAND_KRW, or MILLION_KRW, then disable diagnostic mode";
        return new InvestorFlowVerification(
                InvestorFlowProvider.KIS,
                data.endpoint(),
                data.trId(),
                kisConfiguration.readOnlyEnvironment(),
                tradeDate,
                data.detectedRows(),
                data.availableFields(),
                data.sampleInvestorTypes(),
                data.rawAmountFieldsMasked(),
                data.rawQuantityFieldsMasked(),
                verified ? InvestorFlowAmountUnitStatus.VERIFIED
                        : InvestorFlowAmountUnitStatus.UNVERIFIED,
                List.copyOf(warnings),
                nextAction
        );
    }
}
