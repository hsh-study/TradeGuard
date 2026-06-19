package seokhoon.trade.application.port.in;
import seokhoon.trade.domain.research.*;import java.math.BigDecimal;import java.time.LocalDate;import java.util.List;
public interface ConsensusUseCase {
 EarningsConsensusSnapshot saveEarnings(EarningsConsensusCommand command);TargetPriceConsensusSnapshot saveTargetPrice(TargetPriceConsensusCommand command);
 List<EarningsConsensusSnapshot> importEarningsCsv(byte[] csv);List<TargetPriceConsensusSnapshot> importTargetPriceCsv(byte[] csv);
 List<EarningsConsensusSnapshot> findEarnings(String stockCode,Integer fiscalYear,Integer fiscalQuarter);List<TargetPriceConsensusSnapshot> findTargetPrices(String stockCode);
 record EarningsConsensusCommand(String stockCode,int fiscalYear,int fiscalQuarter,LocalDate consensusDate,BigDecimal expectedRevenue,BigDecimal expectedOperatingIncome,BigDecimal expectedNetIncome,BigDecimal expectedOperatingMargin,Integer analystCount,ConsensusSource source,String providerName){}
 record TargetPriceConsensusCommand(String stockCode,LocalDate consensusDate,BigDecimal targetPrice,BigDecimal currentPrice,BigDecimal upsideRate,Integer analystCount,ConsensusSource source,String providerName){}
}
