package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.order.TradingAccount;

import java.util.List;
import java.util.Optional;

public interface TradingAccountPort {
    TradingAccount save(TradingAccount account);
    List<TradingAccount> findAll();
    Optional<TradingAccount> findById(long id);
    Optional<TradingAccount> findPrimary(KisEnvironment environment);
    Optional<TradingAccount> findPrimary();
    void clearPrimary(KisEnvironment environment);
    void clearPrimary();
}
