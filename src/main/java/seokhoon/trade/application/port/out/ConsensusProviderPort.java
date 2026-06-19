package seokhoon.trade.application.port.out;
import seokhoon.trade.domain.research.*;
import java.time.LocalDate;import java.util.List;
public interface ConsensusProviderPort {
    List<EarningsConsensusSnapshot> fetchEarningsConsensus(String stockCode,LocalDate fromDate,LocalDate toDate);
    List<TargetPriceConsensusSnapshot> fetchTargetPriceConsensus(String stockCode,LocalDate fromDate,LocalDate toDate);
}
