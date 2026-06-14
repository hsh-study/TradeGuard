package seokhoon.trade.adapter.marketdata.kis;

import seokhoon.trade.application.port.out.OperationalMetricsPort;

import java.time.Clock;

class KisAccessTokenProvider extends InMemoryKisAccessTokenProvider {
    KisAccessTokenProvider(
            KisHttpClient httpClient,
            KisProperties properties
    ) {
        this(httpClient,properties,Clock.systemUTC());
    }

    KisAccessTokenProvider(
            KisHttpClient httpClient,
            KisProperties properties,
            Clock clock
    ) {
        super(new KisOAuthTokenClient(httpClient,properties,clock),
                properties,OperationalMetricsPort.noop(),clock);
    }
}
