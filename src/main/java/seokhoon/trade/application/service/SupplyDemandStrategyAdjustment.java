package seokhoon.trade.application.service;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.SupplyDemandSnapshotPort;
import seokhoon.trade.config.SupplyDemandStrategyProperties;
import seokhoon.trade.domain.market.SupplyDemandStatus;
import java.util.ArrayList;
import java.util.List;

@Component
public class SupplyDemandStrategyAdjustment {
    private final SupplyDemandSnapshotPort snapshots; private final SupplyDemandStrategyProperties properties; private final OperationalMetricsPort metrics;
    public SupplyDemandStrategyAdjustment(SupplyDemandSnapshotPort snapshots,SupplyDemandStrategyProperties properties,OperationalMetricsPort metrics){this.snapshots=snapshots;this.properties=properties;this.metrics=metrics;}
    public Assessment assess(String stockCode,String strategy){
        if(!properties.isEnabled())return new Assessment(0,false,List.of());
        var optional=snapshots.findLatestByStockCode(stockCode);
        if(optional.isEmpty()){metrics.recordSupplyDemandStrategyAdjustment(strategy,"insufficient");return new Assessment(0,false,List.of("SUPPLY_DEMAND_DATA_INSUFFICIENT"));}
        var value=optional.get();List<String> reasons=new ArrayList<>(value.reasons());
        return switch(value.status()){
            case STRONG_ACCUMULATION->{reasons.add("SUPPLY_DEMAND_STRONG_ACCUMULATION");metrics.recordSupplyDemandStrategyAdjustment(strategy,"strong");yield new Assessment(properties.getStrongScore(),false,reasons);}
            case DISTRIBUTION->{reasons.add("SUPPLY_DEMAND_DISTRIBUTION");metrics.recordSupplyDemandStrategyAdjustment(strategy,"distribution");yield new Assessment(properties.getDistributionPenalty(),properties.isExcludeDistribution(),reasons);}
            case DATA_INSUFFICIENT->{reasons.add("SUPPLY_DEMAND_DATA_INSUFFICIENT");metrics.recordSupplyDemandStrategyAdjustment(strategy,"insufficient");yield new Assessment(0,false,reasons);}
            case NEUTRAL->{reasons.add("SUPPLY_DEMAND_NEUTRAL");metrics.recordSupplyDemandStrategyAdjustment(strategy,"neutral");yield new Assessment(0,false,reasons);}
        };
    }
    public record Assessment(int scoreAdjustment,boolean excluded,List<String> reasons){}
}
