package seokhoon.trade.application.port.out;

public interface TokenEncryptionPort {
    String encrypt(String plainText);
    String decrypt(String cipherText);
}
