package seokhoon.trade.adapter.research.consensus;
import org.springframework.stereotype.Component;import seokhoon.trade.application.port.out.ConsensusProviderPort;import seokhoon.trade.domain.research.*;import java.time.LocalDate;import java.util.List;
@Component public class DisabledConsensusProviderAdapter implements ConsensusProviderPort {
    public List<EarningsConsensusSnapshot> fetchEarningsConsensus(String stockCode,LocalDate fromDate,LocalDate toDate){return List.of();}
    public List<TargetPriceConsensusSnapshot> fetchTargetPriceConsensus(String stockCode,LocalDate fromDate,LocalDate toDate){return List.of();}
}
