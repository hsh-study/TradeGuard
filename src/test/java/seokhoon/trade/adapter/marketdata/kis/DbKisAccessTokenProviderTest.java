package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.kis.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DbKisAccessTokenProviderTest {
    private static final Instant NOW=Instant.parse("2026-06-15T00:00:00Z");

    @Test
    void storesAndLoadsRealAndDemoSeparately() {
        KisTokenClient client=mock(KisTokenClient.class);
        when(client.issueToken(eq(KisEnvironment.REAL),any(),any()))
                .thenReturn(token(KisEnvironment.REAL,"real"));
        when(client.issueToken(eq(KisEnvironment.DEMO),any(),any()))
                .thenReturn(token(KisEnvironment.DEMO,"demo"));
        FakeStore store=new FakeStore();
        DbKisAccessTokenProvider provider=provider(client,store);

        assertThat(provider.getAccessToken(KisEnvironment.REAL))
                .isEqualTo("real");
        assertThat(provider.getAccessToken(KisEnvironment.DEMO))
                .isEqualTo("demo");
        assertThat(store.rows.get(KisEnvironment.REAL)
                .encryptedAccessToken()).isEqualTo("enc:real");
        assertThat(store.rows.get(KisEnvironment.DEMO)
                .encryptedAccessToken()).isEqualTo("enc:demo");
    }

    @Test
    void lockBusyReusesValidTokenWithoutCallingTokenP() {
        KisTokenClient client=mock(KisTokenClient.class);
        FakeStore store=new FakeStore();
        store.rows.put(KisEnvironment.REAL,stored(KisEnvironment.REAL,"old",
                NOW.plusSeconds(300)));
        store.lockAvailable=false;
        DbKisAccessTokenProvider provider=provider(client,store);

        assertThat(provider.getAccessToken(KisEnvironment.REAL))
                .isEqualTo("old");
        verifyNoInteractions(client);
    }

    @Test
    void refreshFailureKeepsExistingValidToken() {
        KisTokenClient client=mock(KisTokenClient.class);
        when(client.issueToken(any(),any(),any()))
                .thenThrow(new KisApiException("failed"));
        FakeStore store=new FakeStore();
        store.rows.put(KisEnvironment.REAL,stored(KisEnvironment.REAL,"old",
                NOW.plusSeconds(300)));
        DbKisAccessTokenProvider provider=provider(client,store);

        assertThat(provider.getAccessToken(KisEnvironment.REAL))
                .isEqualTo("old");
        assertThat(store.rows.get(KisEnvironment.REAL)
                .encryptedAccessToken()).isEqualTo("enc:old");
    }

    @Test
    void lockPreventsSecondProviderFromIssuingDuplicateToken() {
        KisTokenClient client=mock(KisTokenClient.class);
        FakeStore store=new FakeStore();
        store.lockAvailable=false;
        store.rows.put(KisEnvironment.DEMO,stored(KisEnvironment.DEMO,"shared",
                NOW.plusSeconds(3600)));
        DbKisAccessTokenProvider provider=provider(client,store);

        provider.refresh(KisEnvironment.DEMO);

        verifyNoInteractions(client);
    }

    private static DbKisAccessTokenProvider provider(
            KisTokenClient client,FakeStore store) {
        KisProperties properties=new KisProperties();
        properties.setTokenCacheMode(KisTokenCacheMode.DB);
        properties.setTokenEncryptionKey(Base64.getEncoder()
                .encodeToString(new byte[32]));
        properties.setAppKey("key");
        properties.setAppSecret("secret");
        return new DbKisAccessTokenProvider(client,store,
                new PrefixEncryption(),properties,
                OperationalMetricsPort.noop(),
                Clock.fixed(NOW,ZoneOffset.UTC),"owner");
    }

    private static KisAccessToken token(KisEnvironment environment,
            String value) {
        return new KisAccessToken(environment,value,"Bearer",
                NOW.plusSeconds(3600),NOW,"id");
    }

    private static StoredKisAccessToken stored(KisEnvironment environment,
            String value,
            Instant expiresAt) {
        return new StoredKisAccessToken(environment,"Bearer",
                "enc:"+value,NOW,expiresAt,
                LocalDate.of(2026,6,15),null,null);
    }

    private static class PrefixEncryption implements TokenEncryptionPort {
        public String encrypt(String value){return "enc:"+value;}
        public String decrypt(String value){return value.substring(4);}
    }

    private static class FakeStore implements KisAccessTokenStorePort {
        private final Map<KisEnvironment,StoredKisAccessToken> rows=
                new ConcurrentHashMap<>();
        private boolean lockAvailable=true;
        public Optional<StoredKisAccessToken> findByEnvironment(
                KisEnvironment environment){
            return Optional.ofNullable(rows.get(environment));
        }
        public StoredKisAccessToken save(StoredKisAccessToken token){
            rows.put(token.environment(),token);return token;
        }
        public boolean tryAcquireRefreshLock(KisEnvironment environment,
                String owner,Instant now,Instant staleBefore){
            return lockAvailable;
        }
        public void releaseRefreshLock(KisEnvironment environment,
                String owner,Instant now){}
        public void clear(KisEnvironment environment,Instant now){
            rows.remove(environment);
        }
    }
}
