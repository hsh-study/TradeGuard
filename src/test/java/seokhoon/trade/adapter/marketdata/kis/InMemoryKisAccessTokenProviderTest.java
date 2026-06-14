package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.kis.*;

import java.time.*;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InMemoryKisAccessTokenProviderTest {
    private static final Instant NOW=Instant.parse("2026-06-13T00:00:00Z");

    @Test
    void issuesTokenOnMissAndUsesCacheWhileValid() {
        MutableClock clock=new MutableClock(NOW);
        KisTokenClient client=mock(KisTokenClient.class);
        when(client.issueToken(eq(KisEnvironment.DEMO),any(),any()))
                .thenReturn(token(KisEnvironment.DEMO,"demo",NOW,
                        NOW.plusSeconds(3600)));
        var provider=provider(client,clock);

        assertThat(provider.getAccessToken(KisEnvironment.DEMO))
                .isEqualTo("demo");
        assertThat(provider.getAccessToken(KisEnvironment.DEMO))
                .isEqualTo("demo");

        verify(client,times(1)).issueToken(
                eq(KisEnvironment.DEMO),any(),any());
    }

    @Test
    void refreshesWhenTokenIsNearExpiry() {
        MutableClock clock=new MutableClock(NOW);
        KisTokenClient client=mock(KisTokenClient.class);
        when(client.issueToken(eq(KisEnvironment.DEMO),any(),any()))
                .thenReturn(token(KisEnvironment.DEMO,"first",NOW,
                                NOW.plusSeconds(3600)))
                .thenReturn(token(KisEnvironment.DEMO,"second",
                        NOW.plusSeconds(3100),NOW.plusSeconds(7200)));
        var provider=provider(client,clock);
        provider.getAccessToken(KisEnvironment.DEMO);
        clock.set(NOW.plusSeconds(3100));

        assertThat(provider.getAccessToken(KisEnvironment.DEMO))
                .isEqualTo("second");
        verify(client,times(2)).issueToken(
                eq(KisEnvironment.DEMO),any(),any());
    }

    @Test
    void keepsStillValidTokenWhenDailyRefreshFails() {
        MutableClock clock=new MutableClock(NOW);
        KisTokenClient client=mock(KisTokenClient.class);
        when(client.issueToken(eq(KisEnvironment.REAL),any(),any()))
                .thenReturn(token(KisEnvironment.REAL,"old",NOW,
                        NOW.plus(Duration.ofDays(2))))
                .thenThrow(new KisApiException("issue failed"));
        var provider=provider(client,clock);
        provider.getAccessToken(KisEnvironment.REAL);
        clock.set(NOW.plus(Duration.ofDays(1)));

        assertThat(provider.getAccessToken(KisEnvironment.REAL))
                .isEqualTo("old");
    }

    @Test
    void failsWhenRefreshFailsAndExistingTokenIsExpired() {
        MutableClock clock=new MutableClock(NOW);
        KisTokenClient client=mock(KisTokenClient.class);
        when(client.issueToken(eq(KisEnvironment.REAL),any(),any()))
                .thenReturn(token(KisEnvironment.REAL,"old",NOW,
                        NOW.plusSeconds(700)))
                .thenThrow(new KisApiException("issue failed"));
        var provider=provider(client,clock);
        provider.getAccessToken(KisEnvironment.REAL);
        clock.set(NOW.plusSeconds(701));

        assertThatThrownBy(()->provider.getAccessToken(KisEnvironment.REAL))
                .isInstanceOf(KisApiException.class);
    }

    @Test
    void separatesRealAndDemoTokens() {
        MutableClock clock=new MutableClock(NOW);
        KisTokenClient client=mock(KisTokenClient.class);
        when(client.issueToken(eq(KisEnvironment.REAL),any(),any()))
                .thenReturn(token(KisEnvironment.REAL,"real",NOW,
                        NOW.plusSeconds(3600)));
        when(client.issueToken(eq(KisEnvironment.DEMO),any(),any()))
                .thenReturn(token(KisEnvironment.DEMO,"demo",NOW,
                        NOW.plusSeconds(3600)));
        var provider=provider(client,clock);

        assertThat(provider.getAccessToken(KisEnvironment.REAL))
                .isEqualTo("real");
        assertThat(provider.getAccessToken(KisEnvironment.DEMO))
                .isEqualTo("demo");
    }

    @Test
    void concurrentMissIssuesOnlyOneToken() throws Exception {
        MutableClock clock=new MutableClock(NOW);
        KisTokenClient client=mock(KisTokenClient.class);
        when(client.issueToken(eq(KisEnvironment.DEMO),any(),any()))
                .thenAnswer(invocation->{
                    Thread.sleep(30);
                    return token(KisEnvironment.DEMO,"shared",NOW,
                            NOW.plusSeconds(3600));
                });
        var provider=provider(client,clock);
        try (ExecutorService executor=Executors.newFixedThreadPool(8)) {
            List<Future<String>> futures=java.util.stream.IntStream.range(0,20)
                    .mapToObj(index->executor.submit(()->provider
                            .getAccessToken(KisEnvironment.DEMO)))
                    .toList();
            for (Future<String> future : futures) {
                assertThat(future.get()).isEqualTo("shared");
            }
        }
        verify(client,times(1)).issueToken(
                eq(KisEnvironment.DEMO),any(),any());
    }

    private static InMemoryKisAccessTokenProvider provider(
            KisTokenClient client,Clock clock) {
        KisProperties properties=new KisProperties();
        properties.setAppKey("key");
        properties.setAppSecret("secret");
        return new InMemoryKisAccessTokenProvider(client,properties,
                OperationalMetricsPort.noop(),clock);
    }

    private static KisAccessToken token(KisEnvironment environment,
            String value,Instant issuedAt,Instant expiresAt) {
        return new KisAccessToken(environment,value,"Bearer",expiresAt,
                issuedAt,"sanitized");
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant){this.instant=instant;}
        void set(Instant value){instant=value;}
        @Override public ZoneId getZone(){return ZoneOffset.UTC;}
        @Override public Clock withZone(ZoneId zone){return this;}
        @Override public Instant instant(){return instant;}
    }
}
