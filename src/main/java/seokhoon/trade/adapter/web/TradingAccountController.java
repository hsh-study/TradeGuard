package seokhoon.trade.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
import seokhoon.trade.domain.kis.KisEnvironment;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trading-accounts")
public class TradingAccountController {
    private final TradingAccountManagementUseCase accounts;

    public TradingAccountController(TradingAccountManagementUseCase accounts) { this.accounts = accounts; }

    @GetMapping
    List<TradingAccountManagementUseCase.AccountView> list() { return accounts.list(); }

    @GetMapping("/readiness")
    Map<String, Boolean> readiness() { return Map.of("encryptionConfigured", accounts.encryptionConfigured()); }

    @PostMapping
    TradingAccountManagementUseCase.AccountView create(@Valid @RequestBody CreateRequest request) {
        return accounts.create(new TradingAccountManagementUseCase.CreateAccountCommand(
                request.alias(), request.environment(), request.accountNumber(),
                request.productCode(), request.primaryAccount()));
    }

    @PostMapping("/{id}/primary")
    TradingAccountManagementUseCase.AccountView primary(@PathVariable long id) { return accounts.setPrimary(id); }

    @PostMapping("/{id}/active")
    TradingAccountManagementUseCase.AccountView active(@PathVariable long id,
            @RequestParam boolean value) { return accounts.setActive(id, value); }

    public record CreateRequest(
            @NotBlank String alias,
            @NotNull KisEnvironment environment,
            @NotBlank @Pattern(regexp = "\\d{8}") String accountNumber,
            @NotBlank @Pattern(regexp = "\\d{2}") String productCode,
            boolean primaryAccount
    ) {}
}
