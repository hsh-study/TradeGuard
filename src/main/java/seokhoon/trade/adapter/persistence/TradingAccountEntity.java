package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.application.port.out.TradingAccountEncryptionPort;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.order.TradingAccount;

import java.time.Instant;

@Entity
@Table(name = "trading_accounts")
class TradingAccountEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, length = 100) String alias;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) KisEnvironment environment;
    @Column(name = "encrypted_account_number", nullable = false, length = 512) String encryptedAccountNumber;
    @Column(name = "product_code", nullable = false, length = 2) String productCode;
    @Column(nullable = false) boolean active;
    @Column(name = "primary_account", nullable = false) boolean primaryAccount;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    protected TradingAccountEntity() {}

    TradingAccount toDomain(TradingAccountEncryptionPort encryption) {
        return new TradingAccount(id, alias, environment, encryption.decrypt(encryptedAccountNumber),
                productCode, active, primaryAccount, createdAt, updatedAt);
    }

    void update(TradingAccount account, TradingAccountEncryptionPort encryption) {
        alias = account.alias(); environment = account.environment();
        encryptedAccountNumber = encryption.encrypt(account.accountNumber());
        productCode = account.productCode(); active = account.active();
        primaryAccount = account.primaryAccount(); createdAt = account.createdAt(); updatedAt = account.updatedAt();
    }
}
