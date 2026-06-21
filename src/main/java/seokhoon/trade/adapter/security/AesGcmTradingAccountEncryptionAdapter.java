package seokhoon.trade.adapter.security;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import seokhoon.trade.application.port.out.TradingAccountEncryptionPort;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class AesGcmTradingAccountEncryptionAdapter implements TradingAccountEncryptionPort {
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final String configuredKey;
    private final SecureRandom random = new SecureRandom();

    public AesGcmTradingAccountEncryptionAdapter(
            @Value("${tradeguard.kis.token-encryption-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    @Override public boolean configured() {
        try { key(); return true; } catch (RuntimeException exception) { return false; }
    }

    @Override public String encrypt(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("account number is required");
        byte[] nonce = new byte[NONCE_LENGTH];
        random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("trading account encryption failed", exception);
        }
    }

    @Override public String decrypt(String value) {
        try {
            byte[] payload = Base64.getDecoder().decode(value);
            if (payload.length <= NONCE_LENGTH) throw new IllegalArgumentException("encrypted account is invalid");
            byte[] nonce = Arrays.copyOfRange(payload, 0, NONCE_LENGTH);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(Arrays.copyOfRange(payload, NONCE_LENGTH, payload.length)),
                    StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("trading account decryption failed", exception);
        }
    }

    private SecretKeySpec key() {
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException("KIS token encryption key is not configured");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(configuredKey);
            if (decoded.length != 32) throw new IllegalStateException("encryption key must be 32 bytes");
            return new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("encryption key must be valid Base64", exception);
        }
    }
}
