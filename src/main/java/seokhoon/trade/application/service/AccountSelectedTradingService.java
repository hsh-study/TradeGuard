package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.AccountSelectedTradingUseCase;
import seokhoon.trade.application.port.in.LiveTradingUseCases;
import seokhoon.trade.application.port.in.LiveTradingUseCases.LoadLiveTradingUseCase;
import seokhoon.trade.application.port.in.LiveTradingUseCases.RequestLiveBuyUseCase;
import seokhoon.trade.application.port.in.LiveTradingUseCases.RequestLiveSellUseCase;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.order.LiveOrderRequest;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.domain.position.LivePosition;

@Service
public class AccountSelectedTradingService implements AccountSelectedTradingUseCase {
    private final TradingAccountManagementUseCase accounts;
    private final RequestLiveBuyUseCase buy;
    private final RequestLiveSellUseCase sell;
    private final LoadLiveTradingUseCase trading;

    public AccountSelectedTradingService(TradingAccountManagementUseCase accounts,
            RequestLiveBuyUseCase buy, RequestLiveSellUseCase sell,
            LoadLiveTradingUseCase trading) {
        this.accounts = accounts;
        this.buy = buy;
        this.sell = sell;
        this.trading = trading;
    }

    @Override
    public synchronized LiveOrderRequest buy(BuyCommand command) {
        requireLimit(command.orderType());
        TradingAccountManagementUseCase.AccountView account = account(command.accountId());
        requireRealConfirmation(account.environment(), command.realTradingConfirmed());
        accounts.setPrimary(command.accountId());
        return buy.buy(command.signalId(), command.stockCode(), command.quantity(),
                command.orderPrice(), command.orderType());
    }

    @Override
    public synchronized LiveTradingUseCases.LiveSellResult sell(SellCommand command) {
        TradingAccountManagementUseCase.AccountView account = account(command.accountId());
        requireRealConfirmation(account.environment(), command.realTradingConfirmed());
        if (command.positionId() != null) {
            LivePosition position = trading.position(command.positionId());
            if (position.environment() != null && position.environment() != account.environment()) {
                throw new IllegalArgumentException("selected account environment does not match position");
            }
        }
        accounts.setPrimary(command.accountId());
        return sell.sell(command.positionId(), command.stockCode(), command.quantity(),
                command.orderPrice(), command.reason());
    }

    private TradingAccountManagementUseCase.AccountView account(long accountId) {
        return accounts.list().stream()
                .filter(value -> value.id() == accountId && value.active())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("active trading account not found"));
    }

    private static void requireRealConfirmation(KisEnvironment environment, boolean confirmed) {
        if (environment == KisEnvironment.REAL && !confirmed) {
            throw new IllegalArgumentException("real trading confirmation is required");
        }
    }

    private static void requireLimit(OrderType orderType) {
        if (orderType != OrderType.LIMIT) {
            throw new IllegalArgumentException("Only LIMIT orders are allowed");
        }
    }
}
