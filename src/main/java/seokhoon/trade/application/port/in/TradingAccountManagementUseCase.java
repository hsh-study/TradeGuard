package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.kis.KisEnvironment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TradingAccountManagementUseCase {
    List<AccountView> list();
    AccountView create(CreateAccountCommand command);
    AccountView setPrimary(long id);
    AccountView setActive(long id, boolean active);
    Optional<AccountCredentials> primaryCredentials(KisEnvironment environment);
    Optional<AccountCredentials> primaryCredentials();
    Optional<AccountCredentials> credentials(long id);
    boolean encryptionConfigured();

    record CreateAccountCommand(String alias, KisEnvironment environment,
            String accountNumber, String productCode, boolean primaryAccount) {}
    record AccountView(Long id, String alias, KisEnvironment environment,
            String maskedAccountNumber, String productCode, boolean active,
            boolean primaryAccount, Instant createdAt, Instant updatedAt) {}
    record AccountCredentials(Long id, String alias, KisEnvironment environment,
            String accountNumber, String productCode) {}
}
