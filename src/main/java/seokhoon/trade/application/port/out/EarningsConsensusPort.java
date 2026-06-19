package seokhoon.trade.application.port.out;
import seokhoon.trade.domain.research.EarningsConsensusSnapshot;
import java.util.*;
public interface EarningsConsensusPort {
    EarningsConsensusSnapshot save(EarningsConsensusSnapshot value);
    List<EarningsConsensusSnapshot> find(String stockCode,Integer fiscalYear,Integer fiscalQuarter);
    Optional<EarningsConsensusSnapshot> findLatest(String stockCode,int fiscalYear,int fiscalQuarter);
    List<EarningsConsensusSnapshot> findAllEarnings();
    static EarningsConsensusPort noop(){return new EarningsConsensusPort(){public EarningsConsensusSnapshot save(EarningsConsensusSnapshot v){return v;}public List<EarningsConsensusSnapshot> find(String c,Integer y,Integer q){return List.of();}public Optional<EarningsConsensusSnapshot> findLatest(String c,int y,int q){return Optional.empty();}public List<EarningsConsensusSnapshot> findAllEarnings(){return List.of();}};}
}
