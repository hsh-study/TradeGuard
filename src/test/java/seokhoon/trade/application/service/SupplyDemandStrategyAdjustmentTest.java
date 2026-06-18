package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.SupplyDemandStrategyProperties;
import seokhoon.trade.domain.market.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class SupplyDemandStrategyAdjustmentTest {
    @Test void addsStrongScoreAndReason(){var properties=new SupplyDemandStrategyProperties();properties.setStrongScore(10);var adjustment=new SupplyDemandStrategyAdjustment(new OneSnapshot(snapshot(SupplyDemandStatus.STRONG_ACCUMULATION)),properties,OperationalMetricsPort.noop());var result=adjustment.assess("005930","closing_bet");assertThat(result.scoreAdjustment()).isEqualTo(10);assertThat(result.reasons()).contains("SUPPLY_DEMAND_STRONG_ACCUMULATION");}
    @Test void appliesDistributionPenaltyWithoutExcludingByDefault(){var properties=new SupplyDemandStrategyProperties();properties.setDistributionPenalty(-10);var adjustment=new SupplyDemandStrategyAdjustment(new OneSnapshot(snapshot(SupplyDemandStatus.DISTRIBUTION)),properties,OperationalMetricsPort.noop());var result=adjustment.assess("005930","early_market");assertThat(result.scoreAdjustment()).isEqualTo(-10);assertThat(result.excluded()).isFalse();assertThat(result.reasons()).contains("SUPPLY_DEMAND_DISTRIBUTION");}
    @Test void addsInsufficientReasonWhenSnapshotMissing(){var adjustment=new SupplyDemandStrategyAdjustment(new OneSnapshot(null),new SupplyDemandStrategyProperties(),OperationalMetricsPort.noop());assertThat(adjustment.assess("005930","closing_bet").reasons()).containsExactly("SUPPLY_DEMAND_DATA_INSUFFICIENT");}
    private static StockSupplyDemandSnapshot snapshot(SupplyDemandStatus status){return new StockSupplyDemandSnapshot(1L,"005930",LocalDate.of(2026,6,15),BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,3,3,3,BigDecimal.TWO,BigDecimal.TEN,BigDecimal.ZERO,60,status,List.of("SMART_MONEY_NET_BUY"),Instant.now(),Instant.now());}
    private record OneSnapshot(StockSupplyDemandSnapshot value) implements SupplyDemandSnapshotPort{public StockSupplyDemandSnapshot save(StockSupplyDemandSnapshot v){return v;}public Optional<StockSupplyDemandSnapshot> findByStockCodeAndDate(String c,LocalDate d){return Optional.ofNullable(value);}public Optional<StockSupplyDemandSnapshot> findLatestByStockCode(String c){return Optional.ofNullable(value);}public List<StockSupplyDemandSnapshot> findByTradeDate(LocalDate d){return value==null?List.of():List.of(value);}}
}
