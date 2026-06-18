package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.MarketInvestorFlowPort;
import seokhoon.trade.domain.market.MarketInvestorFlow;
import seokhoon.trade.domain.market.InvestorFlowMarket;
import java.time.LocalDate;
import java.util.List;

@Component
public class MarketInvestorFlowPersistenceAdapter implements MarketInvestorFlowPort {
    private final MarketInvestorFlowJpaRepository markets;
    public MarketInvestorFlowPersistenceAdapter(MarketInvestorFlowJpaRepository markets){this.markets=markets;}
    @Override @Transactional public List<MarketInvestorFlow> saveAll(List<MarketInvestorFlow> values){return values.stream().map(v->{var e=markets.findByMarketAndTradeDateAndInvestorTypeAndSource(v.market(),v.tradeDate(),v.investorType(),v.source()).orElseGet(MarketInvestorFlowEntity::new);e.update(v);return markets.save(e).toDomain();}).toList();}
    @Override public List<MarketInvestorFlow> findByMarketAndDate(InvestorFlowMarket market,LocalDate date){return markets.findByMarketAndTradeDate(market,date).stream().map(MarketInvestorFlowEntity::toDomain).toList();}
    @Override public List<MarketInvestorFlow> findRecentByMarket(InvestorFlowMarket market,LocalDate end,int days){return markets.findByMarketAndTradeDateBetweenOrderByTradeDateDesc(market,end.minusDays(Math.max(days*3L,days)),end).stream().map(MarketInvestorFlowEntity::toDomain).toList();}
}
