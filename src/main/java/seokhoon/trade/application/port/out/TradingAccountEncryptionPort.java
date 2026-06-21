package seokhoon.trade.application.port.out;

public interface TradingAccountEncryptionPort {
    boolean configured();
    String encrypt(String value);
    String decrypt(String value);
}
