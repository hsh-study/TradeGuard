package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.order.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AccountSelectedTradingServiceTest {
    @Test
    void demoBuySelectsAccountAndRoutesLimitOrder() {
        TradingAccountManagementUseCase accounts = mock(TradingAccountManagementUseCase.class);
        var buy = mock(LiveTradingUseCases.RequestLiveBuyUseCase.class);
        when(accounts.list()).thenReturn(List.of(account(1L, KisEnvironment.DEMO)));
        var service = new AccountSelectedTradingService(accounts, buy,
                mock(LiveTradingUseCases.RequestLiveSellUseCase.class),
                mock(LiveTradingUseCases.LoadLiveTradingUseCase.class));

        service.buy(new AccountSelectedTradingUseCase.BuyCommand(1L, null,
                "005930", 1, new BigDecimal("70000"), OrderType.LIMIT, false));

        var ordered = inOrder(accounts, buy);
        ordered.verify(accounts).setPrimary(1L);
        ordered.verify(buy).buy(null, "005930", 1,
                new BigDecimal("70000"), OrderType.LIMIT);
    }

    @Test
    void realBuyRequiresExplicitConfirmationBeforeChangingAccount() {
        TradingAccountManagementUseCase accounts = mock(TradingAccountManagementUseCase.class);
        var buy = mock(LiveTradingUseCases.RequestLiveBuyUseCase.class);
        when(accounts.list()).thenReturn(List.of(account(2L, KisEnvironment.REAL)));
        var service = new AccountSelectedTradingService(accounts, buy,
                mock(LiveTradingUseCases.RequestLiveSellUseCase.class),
                mock(LiveTradingUseCases.LoadLiveTradingUseCase.class));

        assertThatThrownBy(() -> service.buy(new AccountSelectedTradingUseCase.BuyCommand(
                2L, null, "005930", 1, new BigDecimal("70000"),
                OrderType.LIMIT, false))).isInstanceOf(IllegalArgumentException.class);
        verify(accounts, never()).setPrimary(anyLong());
        verifyNoInteractions(buy);
    }

    @Test
    void rejectsMarketOrderForEveryEnvironment() {
        TradingAccountManagementUseCase accounts = mock(TradingAccountManagementUseCase.class);
        var service = new AccountSelectedTradingService(accounts,
                mock(LiveTradingUseCases.RequestLiveBuyUseCase.class),
                mock(LiveTradingUseCases.RequestLiveSellUseCase.class),
                mock(LiveTradingUseCases.LoadLiveTradingUseCase.class));
        assertThatThrownBy(() -> service.buy(new AccountSelectedTradingUseCase.BuyCommand(
                1L, null, "005930", 1, BigDecimal.ONE,
                null, true))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(accounts);
    }

    private static TradingAccountManagementUseCase.AccountView account(
            long id, KisEnvironment environment) {
        Instant now = Instant.parse("2026-06-20T00:00:00Z");
        return new TradingAccountManagementUseCase.AccountView(id, "account", environment,
                "******12", "01", true, false, now, now);
    }
}
