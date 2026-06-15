package seokhoon.trade.adapter.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import seokhoon.trade.adapter.marketdata.kis.KisProperties;
import seokhoon.trade.application.port.out.TokenEncryptionPort;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;

@Component
@ConditionalOnProperty(name="tradeguard.kis.token-cache-mode",
        havingValue="DB")
public class AesGcmTokenEncryptionAdapter
        implements TokenEncryptionPort {
    private static final int NONCE_LENGTH=12;
    private static final int TAG_LENGTH_BITS=128;
    private final SecretKeySpec key;
    private final SecureRandom random;

    @Autowired
    public AesGcmTokenEncryptionAdapter(KisProperties properties) {
        this(properties,new SecureRandom());
    }

    AesGcmTokenEncryptionAdapter(
            KisProperties properties,
            SecureRandom random
    ) {
        this.key=new SecretKeySpec(decodeKey(
                properties.getTokenEncryptionKey()),"AES");
        this.random=random;
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("Token must not be blank");
        }
        byte[] nonce=new byte[NONCE_LENGTH];
        random.nextBytes(nonce);
        try {
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,key,
                    new GCMParameterSpec(TAG_LENGTH_BITS,nonce));
            byte[] encrypted=cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload=new byte[nonce.length+encrypted.length];
            System.arraycopy(nonce,0,payload,0,nonce.length);
            System.arraycopy(encrypted,0,payload,nonce.length,
                    encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("KIS token encryption failed",
                    exception);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        try {
            byte[] payload=Base64.getDecoder().decode(cipherText);
            if (payload.length <= NONCE_LENGTH) {
                throw new IllegalArgumentException(
                        "Encrypted token is invalid");
            }
            byte[] nonce=Arrays.copyOfRange(payload,0,NONCE_LENGTH);
            byte[] encrypted=Arrays.copyOfRange(payload,NONCE_LENGTH,
                    payload.length);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key,
                    new GCMParameterSpec(TAG_LENGTH_BITS,nonce));
            return new String(cipher.doFinal(encrypted),
                    StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("KIS token decryption failed",
                    exception);
        }
    }

    private static byte[] decodeKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "KIS token encryption key is required for DB cache");
        }
        try {
            byte[] decoded=Base64.getDecoder().decode(value);
            if (decoded.length != 32) {
                throw new IllegalStateException(
                        "KIS token encryption key must be 32 bytes");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "KIS token encryption key must be valid Base64",
                    exception);
        }
    }
}
