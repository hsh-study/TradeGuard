package seokhoon.trade.adapter.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import seokhoon.trade.config.InvestorFlowProperties;

@Component("investorFlowProvider")
public class InvestorFlowProviderHealthIndicator implements HealthIndicator {
    private final InvestorFlowProperties properties;

    public InvestorFlowProviderHealthIndicator(InvestorFlowProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (!properties.isProviderEnabled()) {
            return details(Health.up())
                    .withDetail("operationalMode", "DISABLED")
                    .build();
        }
        if (properties.isKisAmountUnitVerified()) {
            return details(Health.up())
                    .withDetail("operationalMode", "READY")
                    .build();
        }
        Health.Builder builder = properties.isImportAutoRun()
                ? Health.outOfService() : Health.unknown();
        return details(builder)
                .withDetail("operationalMode", "AMOUNT_UNIT_UNVERIFIED")
                .build();
    }

    private Health.Builder details(Health.Builder builder) {
        return builder
                .withDetail("providerEnabled", properties.isProviderEnabled())
                .withDetail("providerType", properties.getProviderType())
                .withDetail("amountUnit", properties.getKisAmountUnit().name())
                .withDetail("amountUnitVerified", properties.isKisAmountUnitVerified())
                .withDetail("importAutoRun", properties.isImportAutoRun())
                .withDetail("diagnosticEnabled", properties.isDiagnosticEnabled());
    }
}
