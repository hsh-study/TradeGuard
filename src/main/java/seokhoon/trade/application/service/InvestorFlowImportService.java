package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.ImportInvestorFlowsUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.InvestorFlowProperties;
import seokhoon.trade.domain.market.*;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class InvestorFlowImportService implements ImportInvestorFlowsUseCase {
    private final StockInvestorFlowPort stockFlows; private final MarketInvestorFlowPort marketFlows;
    private final InvestorFlowProviderPort provider; private final InvestorFlowImportHistoryPort histories;
    private final StockPort stocks; private final InvestorFlowProperties properties; private final OperationalMetricsPort metrics;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public InvestorFlowImportService(StockInvestorFlowPort sf, MarketInvestorFlowPort mf,
            InvestorFlowProviderPort provider, InvestorFlowImportHistoryPort histories, StockPort stocks,
            InvestorFlowProperties properties, OperationalMetricsPort metrics) {
        this(sf,mf,provider,histories,stocks,properties,metrics,Clock.systemUTC());
    }
    InvestorFlowImportService(StockInvestorFlowPort sf, MarketInvestorFlowPort mf,
            InvestorFlowProviderPort provider, InvestorFlowImportHistoryPort histories, StockPort stocks,
            InvestorFlowProperties properties, OperationalMetricsPort metrics, Clock clock) {
        this.stockFlows=sf; this.marketFlows=mf; this.provider=provider; this.histories=histories;
        this.stocks=stocks; this.properties=properties; this.metrics=metrics; this.clock=clock;
    }
    @Override @Transactional public InvestorFlowImportHistory importStock(String code, LocalDate date) {
        Instant requested=clock.instant();
        if(!properties.isProviderEnabled()) return history(InvestorFlowImportScope.STOCK,code,null,date,providerType(),InvestorFlowImportStatus.SKIPPED,0,"INVESTOR_FLOW_PROVIDER_DISABLED",requested);
        try { var fetched=provider.fetchStockInvestorFlows(code,date); var normalized=fetched.stream().map(v->normalize(v,code,date)).toList();
            stockFlows.saveAll(normalized); return history(InvestorFlowImportScope.STOCK,code,null,date,providerType(),
                    normalized.isEmpty()?InvestorFlowImportStatus.SKIPPED:InvestorFlowImportStatus.SUCCESS,normalized.size(),normalized.isEmpty()?"NO_PROVIDER_DATA":null,requested);
        } catch(RuntimeException e){return history(InvestorFlowImportScope.STOCK,code,null,date,providerType(),InvestorFlowImportStatus.FAILED,0,sanitize(e.getMessage()),requested);}
    }
    @Override @Transactional public InvestorFlowImportHistory importMarket(InvestorFlowMarket market, LocalDate date) {
        Instant requested=clock.instant();
        if(!properties.isProviderEnabled()) return history(InvestorFlowImportScope.MARKET,null,market,date,providerType(),InvestorFlowImportStatus.SKIPPED,0,"INVESTOR_FLOW_PROVIDER_DISABLED",requested);
        try { var fetched=provider.fetchMarketInvestorFlows(market,date); var normalized=fetched.stream().map(v->normalize(v,market,date)).toList();
            marketFlows.saveAll(normalized); return history(InvestorFlowImportScope.MARKET,null,market,date,providerType(),
                    normalized.isEmpty()?InvestorFlowImportStatus.SKIPPED:InvestorFlowImportStatus.SUCCESS,normalized.size(),normalized.isEmpty()?"NO_PROVIDER_DATA":null,requested);
        } catch(RuntimeException e){return history(InvestorFlowImportScope.MARKET,null,market,date,providerType(),InvestorFlowImportStatus.FAILED,0,sanitize(e.getMessage()),requested);}
    }
    @Override public List<InvestorFlowImportHistory> importWatchlist(LocalDate date){return stocks.findAll().stream().filter(Stock::active).map(s->importStock(s.stockCode(),date)).toList();}
    @Override public List<InvestorFlowImportHistory> importRecentWatchlist(LocalDate baseDate){
        List<InvestorFlowImportHistory> result=new ArrayList<>();
        for(int i=0;i<properties.getLookbackDays();i++){LocalDate d=baseDate.minusDays(i); if(d.getDayOfWeek().getValue()<6) result.addAll(importWatchlist(d));}
        return result;
    }
    @Override @Transactional public InvestorFlowImportHistory importStockCsv(String csv){
        Instant requested=clock.instant(); try {CsvResult<StockInvestorFlow> result=parseStock(csv); stockFlows.saveAll(result.values());
        return history(InvestorFlowImportScope.STOCK,null,null,result.date(),InvestorFlowProvider.CSV,result.status(),result.values().size(),result.reason(),requested);}
        catch(RuntimeException e){return history(InvestorFlowImportScope.STOCK,null,null,LocalDate.now(clock),InvestorFlowProvider.CSV,InvestorFlowImportStatus.FAILED,0,sanitize(e.getMessage()),requested);}
    }
    @Override @Transactional public InvestorFlowImportHistory importMarketCsv(String csv){
        Instant requested=clock.instant(); try {CsvResult<MarketInvestorFlow> result=parseMarket(csv); marketFlows.saveAll(result.values());
        return history(InvestorFlowImportScope.MARKET,null,null,result.date(),InvestorFlowProvider.CSV,result.status(),result.values().size(),result.reason(),requested);}
        catch(RuntimeException e){return history(InvestorFlowImportScope.MARKET,null,null,LocalDate.now(clock),InvestorFlowProvider.CSV,InvestorFlowImportStatus.FAILED,0,sanitize(e.getMessage()),requested);}
    }
    @Override public List<InvestorFlowImportHistory> findHistories(String stockCode){return histories.findRecent(stockCode,100);}
    private InvestorFlowImportHistory history(InvestorFlowImportScope scope,String code,InvestorFlowMarket market,LocalDate date,
            InvestorFlowProvider provider,InvestorFlowImportStatus status,int count,String reason,Instant requested){
        var saved=histories.save(new InvestorFlowImportHistory(null,scope,code,market,date,provider,status,count,reason,requested,clock.instant()));
        metrics.recordInvestorFlowImport(scope.name().toLowerCase(Locale.ROOT),metric(status)); return saved;
    }
    private InvestorFlowProvider providerType(){try{return InvestorFlowProvider.valueOf(properties.getProviderType().toUpperCase(Locale.ROOT));}catch(Exception e){return InvestorFlowProvider.PROVIDER;}}
    private StockInvestorFlow normalize(StockInvestorFlow v,String code,LocalDate date){Instant now=clock.instant(); return new StockInvestorFlow(null,code,date,v.investorType(),v.rawInvestorType(),v.netBuyAmount(),v.netBuyQuantity(),v.buyAmount(),v.sellAmount(),v.buyQuantity(),v.sellQuantity(),providerSource(),now,now);}
    private MarketInvestorFlow normalize(MarketInvestorFlow v,InvestorFlowMarket market,LocalDate date){Instant now=clock.instant(); return new MarketInvestorFlow(null,market,date,v.investorType(),v.rawInvestorType(),v.netBuyAmount(),v.netBuyQuantity(),v.buyAmount(),v.sellAmount(),providerSource(),now,now);}
    private InvestorFlowSource providerSource(){return providerType()==InvestorFlowProvider.KIS?InvestorFlowSource.KIS:InvestorFlowSource.PROVIDER;}
    private CsvResult<StockInvestorFlow> parseStock(String csv){List<String[]> rows=rows(csv); Map<String,Integer> h=header(rows.remove(0),List.of("stockCode","tradeDate","investorType","netBuyAmount","netBuyQuantity")); List<StockInvestorFlow> values=new ArrayList<>(); int bad=0; LocalDate date=LocalDate.now(clock); Instant now=clock.instant();
        for(String[] r:rows)try{date=LocalDate.parse(req(r,h,"tradeDate")); values.add(new StockInvestorFlow(null,req(r,h,"stockCode"),date,type(req(r,h,"investorType")),opt(r,h,"rawInvestorType"),decimal(req(r,h,"netBuyAmount")),Long.parseLong(req(r,h,"netBuyQuantity")),decimalOpt(r,h,"buyAmount"),decimalOpt(r,h,"sellAmount"),longOpt(r,h,"buyQuantity"),longOpt(r,h,"sellQuantity"),source(opt(r,h,"source")),now,now));}catch(RuntimeException e){bad++;}
        return result(values,bad,date);}
    private CsvResult<MarketInvestorFlow> parseMarket(String csv){List<String[]> rows=rows(csv); Map<String,Integer> h=header(rows.remove(0),List.of("market","tradeDate","investorType","netBuyAmount")); List<MarketInvestorFlow> values=new ArrayList<>(); int bad=0; LocalDate date=LocalDate.now(clock); Instant now=clock.instant();
        for(String[] r:rows)try{date=LocalDate.parse(req(r,h,"tradeDate")); values.add(new MarketInvestorFlow(null,InvestorFlowMarket.valueOf(req(r,h,"market").toUpperCase(Locale.ROOT)),date,type(req(r,h,"investorType")),opt(r,h,"rawInvestorType"),decimal(req(r,h,"netBuyAmount")),longOpt(r,h,"netBuyQuantity"),decimalOpt(r,h,"buyAmount"),decimalOpt(r,h,"sellAmount"),source(opt(r,h,"source")),now,now));}catch(RuntimeException e){bad++;}
        return result(values,bad,date);}
    private static <T> CsvResult<T> result(List<T> v,int bad,LocalDate d){var s=bad==0?InvestorFlowImportStatus.SUCCESS:v.isEmpty()?InvestorFlowImportStatus.FAILED:InvestorFlowImportStatus.PARTIAL;return new CsvResult<>(v,s,d,bad==0?null:"invalidRows="+bad);}
    private static List<String[]> rows(String csv){if(csv==null||csv.isBlank())throw new IllegalArgumentException("CSV is empty");var list=new ArrayList<>(csv.lines().filter(l->!l.isBlank()).map(l->Arrays.stream(l.split(",",-1)).map(String::trim).toArray(String[]::new)).toList());if(list.size()<2)throw new IllegalArgumentException("CSV must contain header and rows");return list;}
    private static Map<String,Integer> header(String[] row,List<String> required){Map<String,Integer> h=new HashMap<>();for(int i=0;i<row.length;i++)h.put(row[i],i);required.forEach(k->{if(!h.containsKey(k))throw new IllegalArgumentException("Missing CSV column: "+k);});return h;}
    private static String req(String[] r,Map<String,Integer> h,String k){String v=opt(r,h,k);if(v==null||v.isBlank())throw new IllegalArgumentException("Missing "+k);return v;}
    private static String opt(String[] r,Map<String,Integer> h,String k){Integer i=h.get(k);return i==null||i>=r.length||r[i].isBlank()?null:r[i];}
    private static BigDecimal decimal(String v){return new BigDecimal(v);} private static BigDecimal decimalOpt(String[] r,Map<String,Integer> h,String k){String v=opt(r,h,k);return v==null?null:decimal(v);} private static Long longOpt(String[] r,Map<String,Integer> h,String k){String v=opt(r,h,k);return v==null?null:Long.valueOf(v);}
    static InvestorType type(String raw){String n=raw.trim().toUpperCase(Locale.ROOT).replace(' ','_');return switch(n){case "외국인","FOREIGNER"->InvestorType.FOREIGN;case "기관","기관계","INSTITUTIONAL"->InvestorType.INSTITUTION;case "개인","RETAIL"->InvestorType.INDIVIDUAL;default->InvestorType.valueOf(n);};}
    private static InvestorFlowSource source(String v){return v==null?InvestorFlowSource.CSV:InvestorFlowSource.valueOf(v.toUpperCase(Locale.ROOT));}
    private static String sanitize(String s){if(s==null||s.isBlank())return "investor flow import failed";String v=s.replaceAll("[\\r\\n\\t]"," ");return v.length()>1000?v.substring(0,1000):v;}
    private static String metric(InvestorFlowImportStatus s){return s==InvestorFlowImportStatus.FAILED?"failure":s.name().toLowerCase(Locale.ROOT);}
    private record CsvResult<T>(List<T> values,InvestorFlowImportStatus status,LocalDate date,String reason){}
}
