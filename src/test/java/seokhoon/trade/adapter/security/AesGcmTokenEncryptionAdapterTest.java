package seokhoon.trade.adapter.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import seokhoon.trade.adapter.marketdata.kis.KisProperties;
import seokhoon.trade.domain.kis.KisTokenCacheMode;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class AesGcmTokenEncryptionAdapterTest {
    @Test
    void encryptsAndDecryptsWithDifferentNoncePerCall() {
        KisProperties properties=properties();
        AesGcmTokenEncryptionAdapter adapter=
                new AesGcmTokenEncryptionAdapter(properties);

        String first=adapter.encrypt("raw-secret-token");
        String second=adapter.encrypt("raw-secret-token");

        assertThat(first).isNotEqualTo(second);
        assertThat(first).doesNotContain("raw-secret-token");
        assertThat(adapter.decrypt(first)).isEqualTo("raw-secret-token");
        assertThat(adapter.decrypt(second)).isEqualTo("raw-secret-token");
    }

    @Test
    void rejectsMissingEncryptionKeyInDbMode() {
        KisProperties properties=new KisProperties();
        properties.setTokenCacheMode(KisTokenCacheMode.DB);

        assertThatThrownBy(
                ()->new AesGcmTokenEncryptionAdapter(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("encryption key");
    }

    @Test
    void springContextFailsToStartWithoutDbEncryptionKey() {
        KisProperties properties=new KisProperties();
        properties.setTokenCacheMode(KisTokenCacheMode.DB);
        try (AnnotationConfigApplicationContext context=
                     new AnnotationConfigApplicationContext()) {
            TestPropertyValues.of(
                    "tradeguard.kis.token-cache-mode=DB")
                    .applyTo(context);
            context.registerBean(KisProperties.class,()->properties);
            context.registerBean(AesGcmTokenEncryptionAdapter.class);

            assertThatThrownBy(context::refresh)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasStackTraceContaining(
                            "KIS token encryption key is required");
        }
    }

    private static KisProperties properties() {
        KisProperties properties=new KisProperties();
        properties.setTokenCacheMode(KisTokenCacheMode.DB);
        properties.setTokenEncryptionKey(Base64.getEncoder()
                .encodeToString(new byte[32]));
        return properties;
    }
}
