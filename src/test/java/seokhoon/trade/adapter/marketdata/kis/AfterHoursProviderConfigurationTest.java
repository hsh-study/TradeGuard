package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import seokhoon.trade.adapter.marketdata.DisabledAfterHoursMarketDataAdapter;
import seokhoon.trade.adapter.marketdata.FakeAfterHoursMarketDataAdapter;
import seokhoon.trade.application.port.out.AfterHoursMarketDataPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AfterHoursProviderConfigurationTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(AfterHoursAdapterConfiguration.class)
                    .withBean(KisHttpClient.class, () -> mock(KisHttpClient.class))
                    .withBean(
                            KisAccessTokenProvider.class,
                            () -> mock(KisAccessTokenProvider.class)
                    )
                    .withBean(KisProperties.class, KisProperties::new)
                    .withBean(
                            OperationalMetricsPort.class,
                            OperationalMetricsPort::noop
                    );

    @Test
    void loadsFakeAdapterByDefault() {
        contextRunner.run(context -> assertProvider(
                context.getBean(AfterHoursMarketDataPort.class),
                FakeAfterHoursMarketDataAdapter.class
        ));
    }

    @Test
    void loadsFakeAdapterWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "tradeguard.market-data.after-hours-provider=fake"
                )
                .run(context -> assertProvider(
                        context.getBean(AfterHoursMarketDataPort.class),
                        FakeAfterHoursMarketDataAdapter.class
                ));
    }

    @Test
    void loadsDisabledAdapterWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "tradeguard.market-data.after-hours-provider=disabled"
                )
                .run(context -> assertProvider(
                        context.getBean(AfterHoursMarketDataPort.class),
                        DisabledAfterHoursMarketDataAdapter.class
                ));
    }

    @Test
    void loadsKisAdapterWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "tradeguard.market-data.after-hours-provider=kis"
                )
                .run(context -> assertProvider(
                        context.getBean(AfterHoursMarketDataPort.class),
                        KisAfterHoursMarketDataAdapter.class
                ));
    }

    @Test
    void legacyDisabledFlagStillLoadsDisabledAdapter() {
        contextRunner
                .withPropertyValues(
                        "tradeguard.market-data.after-hours-enabled=false"
                )
                .run(context -> assertProvider(
                        context.getBean(AfterHoursMarketDataPort.class),
                        DisabledAfterHoursMarketDataAdapter.class
                ));
    }

    private static void assertProvider(
            AfterHoursMarketDataPort port,
            Class<?> expectedType
    ) {
        assertThat(port).isInstanceOf(expectedType);
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            FakeAfterHoursMarketDataAdapter.class,
            DisabledAfterHoursMarketDataAdapter.class,
            KisAfterHoursMarketDataAdapter.class
    })
    static class AfterHoursAdapterConfiguration {
    }
}
