package seokhoon.trade.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyMarketStrategyPropertiesTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsConfiguredValues() {
        contextRunner.withPropertyValues(
                        "tradeguard.early-market.strategy.opening.entry-threshold=80",
                        "tradeguard.early-market.strategy.opening.max-candidates=5",
                        "tradeguard.early-market.strategy.follow-up.exclude-drawdown-from-high=-2.5",
                        "tradeguard.early-market.strategy.pre-open.after-hours-rise-threshold=4.0",
                        "tradeguard.early-market.strategy.follow-up.caution-when-previous-high-not-broken=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    EarlyMarketStrategyProperties properties =
                            context.getBean(EarlyMarketStrategyProperties.class);
                    assertThat(properties.getOpening().getEntryThreshold())
                            .isEqualTo(80);
                    assertThat(properties.getOpening().getMaxCandidates())
                            .isEqualTo(5);
                    assertThat(properties.getFollowUp()
                            .getExcludeDrawdownFromHigh())
                            .isEqualByComparingTo("-2.5");
                    assertThat(properties.getPreOpen()
                            .getAfterHoursRiseThreshold())
                            .isEqualByComparingTo("4.0");
                    assertThat(properties.getFollowUp()
                            .isCautionWhenPreviousHighNotBroken()).isFalse();
                });
    }

    @Test
    void rejectsInvalidConfiguredValues() {
        contextRunner.withPropertyValues(
                        "tradeguard.early-market.strategy.opening.entry-threshold=101"
                )
                .run(context -> assertThat(context).hasFailed());

        contextRunner.withPropertyValues(
                        "tradeguard.early-market.strategy.opening.max-candidates=0"
                )
                .run(context -> assertThat(context).hasFailed());

        contextRunner.withPropertyValues(
                        "tradeguard.early-market.strategy.follow-up.exclude-drawdown-from-high=-0.5",
                        "tradeguard.early-market.strategy.follow-up.caution-drawdown-from-high=-1.0"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EarlyMarketStrategyProperties.class)
    static class PropertiesConfiguration {
    }
}
