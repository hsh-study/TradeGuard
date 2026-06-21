package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.stock.Stock;

public interface ManageStockUseCase {
    Stock changeActive(String stockCode, boolean active);
    Stock removeFromWatchlist(String stockCode);
}
