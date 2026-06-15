package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import seokhoon.trade.application.port.out.KisAccessTokenStorePort;
import seokhoon.trade.domain.kis.*;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties={
        "tradeguard.kis.token-cache-mode=DB",
        "tradeguard.kis.token-encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "tradeguard.kis.app-key=test-key",
        "tradeguard.kis.app-secret=test-secret"
})
@ActiveProfiles("test")
class KisAccessTokenPersistenceIntegrationTest {
    @Autowired KisAccessTokenStorePort store;

    @Test
    void storesSeparateRowsAndCoordinatesRefreshLease() {
        Instant now=Instant.parse("2026-06-15T00:00:00Z");
        store.save(token(KisEnvironment.REAL,"cipher-real",now));
        store.save(token(KisEnvironment.DEMO,"cipher-demo",now));

        assertThat(store.findByEnvironment(KisEnvironment.REAL))
                .get().extracting(StoredKisAccessToken::encryptedAccessToken)
                .isEqualTo("cipher-real");
        assertThat(store.findByEnvironment(KisEnvironment.DEMO))
                .get().extracting(StoredKisAccessToken::encryptedAccessToken)
                .isEqualTo("cipher-demo");

        assertThat(store.tryAcquireRefreshLock(KisEnvironment.REAL,
                "instance-a",now,now.minusSeconds(120))).isTrue();
        assertThat(store.tryAcquireRefreshLock(KisEnvironment.REAL,
                "instance-b",now.plusSeconds(1),
                now.minusSeconds(119))).isFalse();
        store.releaseRefreshLock(KisEnvironment.REAL,"instance-a",
                now.plusSeconds(2));
        assertThat(store.tryAcquireRefreshLock(KisEnvironment.REAL,
                "instance-b",now.plusSeconds(3),
                now.minusSeconds(117))).isTrue();
        assertThat(store.tryAcquireRefreshLock(KisEnvironment.REAL,
                "instance-c",now.plusSeconds(124),
                now.plusSeconds(4))).isTrue();
    }

    private static StoredKisAccessToken token(
            KisEnvironment environment,String cipher,Instant now) {
        return new StoredKisAccessToken(environment,"Bearer",cipher,now,
                now.plusSeconds(3600),LocalDate.of(2026,6,15),null,null);
    }
}
