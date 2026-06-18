package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.InvestorFlowProperties;
import seokhoon.trade.domain.market.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class SupplyDemandAnalysisServiceTest {
    private static final LocalDate DATE=LocalDate.of(2026,6,15);private static final Instant NOW=Instant.parse("2026-06-15T00:00:00Z");
    @Test void calculatesSmartMoneyConsecutiveDaysAndStrongAccumulation(){var flows=new FlowPort();for(int i=0;i<5;i++){flows.add(flow(DATE.minusDays(i),InvestorType.FOREIGN,"100"));flows.add(flow(DATE.minusDays(i),InvestorType.INSTITUTION,"200"));flows.add(flow(DATE.minusDays(i),InvestorType.INDIVIDUAL,"-300"));}var snapshots=new Snapshots();var result=service(flows,snapshots).analyzeStock("005930",DATE);
        assertThat(result.smartMoneyNetBuyAmount()).isEqualByComparingTo("300");assertThat(result.smartMoney5dayNetBuyAmount()).isEqualByComparingTo("1500");assertThat(result.consecutiveForeignBuyDays()).isEqualTo(5);assertThat(result.consecutiveInstitutionBuyDays()).isEqualTo(5);assertThat(result.consecutiveCombinedSmartMoneyBuyDays()).isEqualTo(5);assertThat(result.supplyDemandScore()).isEqualTo(85);assertThat(result.status()).isEqualTo(SupplyDemandStatus.STRONG_ACCUMULATION);}
    @Test void individualDominanceAndJointSmartMoneySellingAreDistribution(){var flows=new FlowPort();for(int i=0;i<3;i++){flows.add(flow(DATE.minusDays(i),InvestorType.FOREIGN,"-100"));flows.add(flow(DATE.minusDays(i),InvestorType.INSTITUTION,"-100"));flows.add(flow(DATE.minusDays(i),InvestorType.INDIVIDUAL,"1000"));}var result=service(flows,new Snapshots()).analyzeStock("005930",DATE);assertThat(result.status()).isEqualTo(SupplyDemandStatus.DISTRIBUTION);assertThat(result.supplyDemandScore()).isEqualTo(-45);assertThat(result.reasons()).contains("INDIVIDUAL_DOMINANCE_FOREIGN_INSTITUTION_SELL","FOREIGN_INSTITUTION_JOINT_SELL");assertThat(result.individualDominanceRatio()).isEqualByComparingTo("0.833333");}
    @Test void fewerThanThreeDaysIsInsufficient(){var flows=new FlowPort();flows.add(flow(DATE,InvestorType.FOREIGN,"100"));var result=service(flows,new Snapshots()).analyzeStock("005930",DATE);assertThat(result.status()).isEqualTo(SupplyDemandStatus.DATA_INSUFFICIENT);}
    @Test void unverifiedEnabledKisProviderBlocksAnalysisBeforeReadOrSnapshotWrite(){var flows=mock(StockInvestorFlowPort.class);var snapshots=mock(SupplyDemandSnapshotPort.class);var properties=new InvestorFlowProperties();properties.setProviderEnabled(true);var service=new SupplyDemandAnalysisService(flows,snapshots,mock(StockPort.class),properties,OperationalMetricsPort.noop(),Clock.fixed(NOW,ZoneOffset.UTC));assertThatThrownBy(()->service.analyzeStock("005930",DATE)).isInstanceOf(InvestorFlowAmountUnitUnverifiedException.class);verifyNoInteractions(flows,snapshots);}
    private static SupplyDemandAnalysisService service(FlowPort flows,Snapshots snapshots){var p=new InvestorFlowProperties();p.setLookbackDays(20);return new SupplyDemandAnalysisService(flows,snapshots,mock(StockPort.class),p,OperationalMetricsPort.noop(),Clock.fixed(NOW,ZoneOffset.UTC));}
    private static StockInvestorFlow flow(LocalDate date,InvestorType type,String amount){return new StockInvestorFlow(null,"005930",date,type,null,new BigDecimal(amount),1L,null,null,null,null,InvestorFlowSource.KIS,NOW,NOW);}
    private static class FlowPort implements StockInvestorFlowPort{List<StockInvestorFlow> values=new ArrayList<>();void add(StockInvestorFlow v){values.add(v);}public List<StockInvestorFlow> saveAll(List<StockInvestorFlow> v){values.addAll(v);return v;}public List<StockInvestorFlow> findByStockCodeAndDate(String c,LocalDate d){return values.stream().filter(v->v.tradeDate().equals(d)).toList();}public List<StockInvestorFlow> findRecentByStockCode(String c,LocalDate d,int days){return values;}}
    private static class Snapshots implements SupplyDemandSnapshotPort{StockSupplyDemandSnapshot value;public StockSupplyDemandSnapshot save(StockSupplyDemandSnapshot v){value=v;return v;}public Optional<StockSupplyDemandSnapshot> findByStockCodeAndDate(String c,LocalDate d){return Optional.ofNullable(value);}public Optional<StockSupplyDemandSnapshot> findLatestByStockCode(String c){return Optional.ofNullable(value);}public List<StockSupplyDemandSnapshot> findByTradeDate(LocalDate d){return value==null?List.of():List.of(value);}}
}
