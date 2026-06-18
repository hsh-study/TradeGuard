package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.AnalyzeSupplyDemandUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.InvestorFlowProperties;
import seokhoon.trade.domain.market.*;
import seokhoon.trade.domain.stock.Stock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

@Service
public class SupplyDemandAnalysisService implements AnalyzeSupplyDemandUseCase {
    private static final Set<InvestorType> INSTITUTIONS=EnumSet.of(InvestorType.INSTITUTION,
            InvestorType.FINANCIAL_INVESTMENT,InvestorType.INSURANCE,InvestorType.INVESTMENT_TRUST,
            InvestorType.BANK,InvestorType.PENSION_FUND,InvestorType.PRIVATE_EQUITY,InvestorType.OTHER_INSTITUTION);
    private final StockInvestorFlowPort flows; private final SupplyDemandSnapshotPort snapshots;
    private final StockPort stocks; private final InvestorFlowProperties properties; private final OperationalMetricsPort metrics; private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public SupplyDemandAnalysisService(StockInvestorFlowPort flows,SupplyDemandSnapshotPort snapshots,
            StockPort stocks,InvestorFlowProperties properties,OperationalMetricsPort metrics){this(flows,snapshots,stocks,properties,metrics,Clock.systemUTC());}
    SupplyDemandAnalysisService(StockInvestorFlowPort flows,SupplyDemandSnapshotPort snapshots,
            StockPort stocks,InvestorFlowProperties properties,OperationalMetricsPort metrics,Clock clock){this.flows=flows;this.snapshots=snapshots;this.stocks=stocks;this.properties=properties;this.metrics=metrics;this.clock=clock;}
    @Override @Transactional public StockSupplyDemandSnapshot analyzeStock(String code,LocalDate date){
        try {var recent=flows.findRecentByStockCode(code,date,properties.getLookbackDays()); var byDate=aggregate(recent);
            if(byDate.size()<3){var value=snapshot(code,date,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,0,0,0,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,0,SupplyDemandStatus.DATA_INSUFFICIENT,List.of("INVESTOR_FLOW_DATA_LESS_THAN_3_DAYS"));metrics.recordSupplyDemandAnalysis("insufficient");return snapshots.save(value);}
            List<LocalDate> dates=byDate.keySet().stream().sorted(Comparator.reverseOrder()).limit(properties.getLookbackDays()).toList(); Daily today=byDate.getOrDefault(date,byDate.get(dates.get(0)));
            if(dates.stream().limit(3).map(byDate::get).anyMatch(daily -> !daily.hasCoreAmounts())){var value=snapshot(code,date,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,0,0,0,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,0,SupplyDemandStatus.DATA_INSUFFICIENT,List.of("INVESTOR_FLOW_AMOUNT_DATA_INSUFFICIENT"));metrics.recordSupplyDemandAnalysis("insufficient");return snapshots.save(value);}
            int foreignDays=consecutive(dates,byDate,d->d.foreign.signum()>0); int institutionDays=consecutive(dates,byDate,d->d.institution.signum()>0); int combinedDays=consecutive(dates,byDate,d->d.smart().signum()>0);
            BigDecimal smart5=dates.stream().limit(5).map(d->byDate.get(d).smart()).reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal totalAbs=today.foreign.abs().add(today.institution.abs()).add(today.individual.abs()); BigDecimal ratio=totalAbs.signum()==0?BigDecimal.ZERO:today.individual.abs().divide(totalAbs,6,RoundingMode.HALF_UP);
            int score=0;List<String> reasons=new ArrayList<>();
            if(today.foreign.signum()>0){score+=15;reasons.add("FOREIGN_NET_BUY");} if(today.institution.signum()>0){score+=15;reasons.add("INSTITUTION_NET_BUY");}
            if(foreignDays>=3){score+=10;reasons.add("FOREIGN_CONSECUTIVE_BUY_3D");} if(institutionDays>=3){score+=10;reasons.add("INSTITUTION_CONSECUTIVE_BUY_3D");}
            if(today.smart().signum()>0){score+=20;reasons.add("SMART_MONEY_NET_BUY");} if(smart5.signum()>0){score+=15;reasons.add("SMART_MONEY_5D_NET_BUY");}
            if(today.individual.signum()>0&&today.foreign.signum()<0&&today.institution.signum()<0){score-=20;reasons.add("INDIVIDUAL_DOMINANCE_FOREIGN_INSTITUTION_SELL");}
            if(today.foreign.signum()<0&&today.institution.signum()<0){score-=25;reasons.add("FOREIGN_INSTITUTION_JOINT_SELL");}
            SupplyDemandStatus status=score>=50?SupplyDemandStatus.STRONG_ACCUMULATION:score>=20?SupplyDemandStatus.NEUTRAL:SupplyDemandStatus.DISTRIBUTION;
            var saved=snapshots.save(snapshot(code,date,today.foreign,today.institution,today.individual,foreignDays,institutionDays,combinedDays,today.smart(),smart5,ratio,score,status,reasons));metrics.recordSupplyDemandAnalysis("success");return saved;
        } catch(RuntimeException e){metrics.recordSupplyDemandAnalysis("failure");throw e;}
    }
    @Override public List<StockSupplyDemandSnapshot> analyzeStocks(List<String> codes,LocalDate date){return codes.stream().distinct().map(c->analyzeStock(c,date)).toList();}
    @Override public List<StockSupplyDemandSnapshot> analyzeWatchlist(LocalDate date){return analyzeStocks(stocks.findAll().stream().filter(Stock::active).map(Stock::stockCode).toList(),date);}
    private StockSupplyDemandSnapshot snapshot(String c,LocalDate d,BigDecimal f,BigDecimal i,BigDecimal p,int fd,int id,int cd,BigDecimal sm,BigDecimal sm5,BigDecimal ratio,int score,SupplyDemandStatus status,List<String> reasons){var now=clock.instant();return new StockSupplyDemandSnapshot(null,c,d,f,i,p,fd,id,cd,sm,sm5,ratio,score,status,reasons,now,now);}
    private static Map<LocalDate,Daily> aggregate(List<StockInvestorFlow> values){
        Map<String,StockInvestorFlow> selected=new HashMap<>();
        for(var value:values){String key=value.tradeDate()+":"+value.investorType();selected.merge(key,value,(left,right)->sourceRank(right.source())>sourceRank(left.source())?right:left);}
        Map<LocalDate,Daily> result=new HashMap<>();for(var v:selected.values()){Daily d=result.computeIfAbsent(v.tradeDate(),x->new Daily());if(v.investorType()==InvestorType.FOREIGN){if(v.netBuyAmount()!=null){d.foreign=d.foreign.add(v.netBuyAmount());d.foreignAmount=true;}}else if(v.investorType()==InvestorType.INDIVIDUAL){if(v.netBuyAmount()!=null){d.individual=d.individual.add(v.netBuyAmount());d.individualAmount=true;}}else if(INSTITUTIONS.contains(v.investorType())&&v.netBuyAmount()!=null){d.institution=d.institution.add(v.netBuyAmount());d.institutionAmount=true;}}return result;}
    private static int sourceRank(InvestorFlowSource source){return switch(source){case KIS,PROVIDER->3;case CSV->2;case MANUAL->1;};}
    private static int consecutive(List<LocalDate> dates,Map<LocalDate,Daily> map,java.util.function.Predicate<Daily> positive){int count=0;for(LocalDate date:dates){if(!positive.test(map.get(date)))break;count++;}return count;}
    private static final class Daily {BigDecimal foreign=BigDecimal.ZERO,institution=BigDecimal.ZERO,individual=BigDecimal.ZERO;boolean foreignAmount,institutionAmount,individualAmount;BigDecimal smart(){return foreign.add(institution);}boolean hasCoreAmounts(){return foreignAmount&&institutionAmount&&individualAmount;}}
}
