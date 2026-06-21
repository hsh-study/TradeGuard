package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
import seokhoon.trade.application.port.out.TradingAccountEncryptionPort;
import seokhoon.trade.application.port.out.TradingAccountPort;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.order.TradingAccount;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TradingAccountManagementService implements TradingAccountManagementUseCase {
    private final TradingAccountPort accounts;
    private final TradingAccountEncryptionPort encryption;

    public TradingAccountManagementService(TradingAccountPort accounts,
            TradingAccountEncryptionPort encryption) {
        this.accounts = accounts;
        this.encryption = encryption;
    }

    @Override @Transactional(readOnly = true)
    public List<AccountView> list() { return accounts.findAll().stream().map(this::view).toList(); }

    @Override @Transactional
    public AccountView create(CreateAccountCommand command) {
        requireEncryption();
        Instant now = Instant.now();
        TradingAccount account = new TradingAccount(null, command.alias(), command.environment(),
                command.accountNumber(), command.productCode(), true,
                command.primaryAccount(), now, now);
        if (command.primaryAccount()) accounts.clearPrimary();
        return view(accounts.save(account));
    }

    @Override @Transactional
    public AccountView setPrimary(long id) {
        TradingAccount current = required(id);
        if (!current.active()) throw new IllegalArgumentException("inactive account cannot be primary");
        accounts.clearPrimary();
        return view(accounts.save(copy(current, true, true)));
    }

    @Override @Transactional
    public AccountView setActive(long id, boolean active) {
        TradingAccount current = required(id);
        return view(accounts.save(copy(current, active, active && current.primaryAccount())));
    }

    @Override @Transactional(readOnly = true)
    public Optional<AccountCredentials> primaryCredentials(KisEnvironment environment) {
        return accounts.findPrimary(environment).filter(TradingAccount::active)
                .map(a -> new AccountCredentials(a.id(), a.alias(), a.environment(),
                        a.accountNumber(), a.productCode()));
    }
    @Override @Transactional(readOnly = true)
    public Optional<AccountCredentials> primaryCredentials() {
        return accounts.findPrimary().filter(TradingAccount::active)
                .map(a -> new AccountCredentials(a.id(), a.alias(), a.environment(), a.accountNumber(), a.productCode()));
    }
    @Override @Transactional(readOnly = true)
    public Optional<AccountCredentials> credentials(long id) {
        return accounts.findById(id).filter(TradingAccount::active)
                .map(a -> new AccountCredentials(a.id(), a.alias(), a.environment(),
                        a.accountNumber(), a.productCode()));
    }

    @Override public boolean encryptionConfigured() { return encryption.configured(); }

    private TradingAccount required(long id) {
        requireEncryption();
        return accounts.findById(id).orElseThrow(() -> new IllegalArgumentException("trading account not found"));
    }

    private void requireEncryption() {
        if (!encryption.configured()) throw new IllegalStateException("KIS_TOKEN_ENCRYPTION_KEY is required for DB account management");
    }

    private TradingAccount copy(TradingAccount a, boolean active, boolean primary) {
        return new TradingAccount(a.id(), a.alias(), a.environment(), a.accountNumber(),
                a.productCode(), active, primary, a.createdAt(), Instant.now());
    }

    private AccountView view(TradingAccount a) {
        return new AccountView(a.id(), a.alias(), a.environment(), a.maskedAccountNumber(),
                a.productCode(), a.active(), a.primaryAccount(), a.createdAt(), a.updatedAt());
    }
}
