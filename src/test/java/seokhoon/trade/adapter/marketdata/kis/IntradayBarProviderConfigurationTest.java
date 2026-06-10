package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import seokhoon.trade.adapter.marketdata.FakeIntradayBarAdapter;
import seokhoon.trade.application.port.out.IntradayBarPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IntradayBarProviderConfigurationTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(IntradayAdapterConfiguration.class)
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
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(IntradayBarPort.class);
            assertThat(context.getBean(IntradayBarPort.class))
                    .isInstanceOf(FakeIntradayBarAdapter.class);
        });
    }

    @Test
    void loadsKisAdapterWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "tradeguard.market-data.intraday-provider=kis"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(IntradayBarPort.class);
                    assertThat(context.getBean(IntradayBarPort.class))
                            .isInstanceOf(KisIntradayBarAdapter.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({FakeIntradayBarAdapter.class, KisIntradayBarAdapter.class})
    static class IntradayAdapterConfiguration {
    }
}
