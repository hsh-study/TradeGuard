package seokhoon.trade.adapter.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.GetWatchlistPortfolioUseCase;

import java.util.List;

@RestController
@RequestMapping("/api/operations/portfolio")
public class WatchlistPortfolioController {
    private final GetWatchlistPortfolioUseCase useCase;

    public WatchlistPortfolioController(GetWatchlistPortfolioUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/watchlist")
    List<GetWatchlistPortfolioUseCase.WatchlistItem> watchlist() {
        return useCase.watchlist();
    }

    @GetMapping("/holdings")
    List<GetWatchlistPortfolioUseCase.HoldingItem> holdings(
            @RequestParam(required = false) Long accountId) {
        return accountId == null ? useCase.holdings() : useCase.holdings(accountId);
    }
}
