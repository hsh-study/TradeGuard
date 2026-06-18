package seokhoon.trade.adapter.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.market.*;
import seokhoon.trade.domain.stock.Market;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class InvestorFlowPersistenceAdapter implements StockInvestorFlowPort, InvestorFlowImportHistoryPort {
    private final StockInvestorFlowJpaRepository stocks;
    private final InvestorFlowImportHistoryJpaRepository histories;

    public InvestorFlowPersistenceAdapter(StockInvestorFlowJpaRepository stocks,
            InvestorFlowImportHistoryJpaRepository histories) {
        this.stocks=stocks; this.histories=histories;
    }

    @Override @Transactional
    public List<StockInvestorFlow> saveAll(List<StockInvestorFlow> values) {
        return values.stream().map(v -> {
            var e=stocks.findByStockCodeAndTradeDateAndInvestorTypeAndSource(v.stockCode(), v.tradeDate(), v.investorType(), v.source())
                    .orElseGet(StockInvestorFlowEntity::new); e.update(v); return stocks.save(e).toDomain();
        }).toList();
    }
    @Override public List<StockInvestorFlow> findByStockCodeAndDate(String code, LocalDate date) {
        return stocks.findByStockCodeAndTradeDate(code,date).stream().map(StockInvestorFlowEntity::toDomain).toList();
    }
    @Override public List<StockInvestorFlow> findRecentByStockCode(String code, LocalDate end, int days) {
        return stocks.findByStockCodeAndTradeDateBetweenOrderByTradeDateDesc(code,end.minusDays(Math.max(days*3L,days)),end)
                .stream().map(StockInvestorFlowEntity::toDomain).toList();
    }
    @Override public InvestorFlowImportHistory save(InvestorFlowImportHistory v) {
        return histories.save(InvestorFlowImportHistoryEntity.from(v)).toDomain();
    }
    @Override public List<InvestorFlowImportHistory> findRecent(String stockCode, int limit) {
        var page=PageRequest.of(0,limit,Sort.by(Sort.Direction.DESC,"requestedAt"));
        var values=stockCode==null || stockCode.isBlank()?histories.findAllBy(page):histories.findByStockCode(stockCode,page);
        return values.stream().map(InvestorFlowImportHistoryEntity::toDomain).toList();
    }
}
