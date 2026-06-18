package seokhoon.trade.adapter.health;

import org.junit.jupiter.api.Test;
import seokhoon.trade.config.InvestorFlowProperties;
import seokhoon.trade.domain.market.KisInvestorFlowAmountUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InvestorFlowProviderHealthIndicatorTest {
    @Test
    void disabledProviderIsUp() {
        var health = new InvestorFlowProviderHealthIndicator(
                new InvestorFlowProperties()).health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("operationalMode", "DISABLED");
    }

    @Test
    void verifiedProviderIsUp() {
        InvestorFlowProperties properties = new InvestorFlowProperties();
        properties.setProviderEnabled(true);
        properties.setKisAmountUnit(KisInvestorFlowAmountUnit.KRW);

        var health = new InvestorFlowProviderHealthIndicator(properties).health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("operationalMode", "READY");
    }

    @Test
    void unverifiedAutoRunProviderIsOutOfServiceWithoutSecrets() {
        InvestorFlowProperties properties = new InvestorFlowProperties();
        properties.setProviderEnabled(true);
        properties.setImportAutoRun(true);

        var health = new InvestorFlowProviderHealthIndicator(properties).health();

        assertThat(health.getStatus().getCode()).isEqualTo("OUT_OF_SERVICE");
        assertThat(health.getDetails().toString())
                .doesNotContain("token", "appKey", "appSecret", "account", "header");
    }
}
