package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
import seokhoon.trade.application.port.out.TradingAccountEncryptionPort;
import seokhoon.trade.application.port.out.TradingAccountPort;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.order.TradingAccount;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class TradingAccountManagementServiceTest {
    @Test void storesMultipleAccountsAndSelectsOneTradingAccountGlobally() {
        MemoryPort port = new MemoryPort();
        TradingAccountManagementService service = new TradingAccountManagementService(port, encryption(true));

        var demo = service.create(command("모의", KisEnvironment.DEMO, "11112222", true));
        var real1 = service.create(command("실전-1", KisEnvironment.REAL, "33334444", true));
        var real2 = service.create(command("실전-2", KisEnvironment.REAL, "55556666", false));
        service.setPrimary(real2.id());

        assertThat(service.primaryCredentials()).get()
                .extracting(TradingAccountManagementUseCase.AccountCredentials::accountNumber)
                .isEqualTo("55556666");
        assertThat(service.list()).extracting(TradingAccountManagementUseCase.AccountView::maskedAccountNumber)
                .containsExactlyInAnyOrder("******22", "******44", "******66");
        assertThat(service.list()).filteredOn(TradingAccountManagementUseCase.AccountView::primaryAccount)
                .extracting(TradingAccountManagementUseCase.AccountView::id)
                .containsExactly(real2.id())
                .doesNotContain(demo.id(), real1.id());
    }

    @Test void refusesRegistrationWithoutEncryptionKey() {
        TradingAccountManagementService service = new TradingAccountManagementService(
                new MemoryPort(), encryption(false));
        assertThatThrownBy(() -> service.create(command("실전", KisEnvironment.REAL, "12345678", true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KIS_TOKEN_ENCRYPTION_KEY");
    }

    private static TradingAccountManagementUseCase.CreateAccountCommand command(
            String alias, KisEnvironment environment, String number, boolean primary) {
        return new TradingAccountManagementUseCase.CreateAccountCommand(alias, environment, number, "01", primary);
    }

    private static TradingAccountEncryptionPort encryption(boolean configured) {
        return new TradingAccountEncryptionPort() {
            public boolean configured() { return configured; }
            public String encrypt(String value) { return "encrypted"; }
            public String decrypt(String value) { return value; }
        };
    }

    private static final class MemoryPort implements TradingAccountPort {
        private final Map<Long, TradingAccount> values = new LinkedHashMap<>();
        private long sequence;
        public TradingAccount save(TradingAccount a) {
            TradingAccount saved = a.id() == null ? new TradingAccount(++sequence, a.alias(), a.environment(),
                    a.accountNumber(), a.productCode(), a.active(), a.primaryAccount(), a.createdAt(), a.updatedAt()) : a;
            values.put(saved.id(), saved); return saved;
        }
        public List<TradingAccount> findAll() { return List.copyOf(values.values()); }
        public Optional<TradingAccount> findById(long id) { return Optional.ofNullable(values.get(id)); }
        public Optional<TradingAccount> findPrimary(KisEnvironment environment) { return values.values().stream()
                .filter(a -> a.environment() == environment && a.active() && a.primaryAccount()).findFirst(); }
        public Optional<TradingAccount> findPrimary() { return values.values().stream()
                .filter(a -> a.active() && a.primaryAccount()).findFirst(); }
        public void clearPrimary(KisEnvironment environment) { values.replaceAll((id, a) -> a.environment() != environment ? a
                : new TradingAccount(a.id(), a.alias(), a.environment(), a.accountNumber(), a.productCode(),
                a.active(), false, a.createdAt(), a.updatedAt())); }
        public void clearPrimary() { values.replaceAll((id, a) -> new TradingAccount(a.id(), a.alias(),
                a.environment(), a.accountNumber(), a.productCode(), a.active(), false,
                a.createdAt(), a.updatedAt())); }
    }
}
