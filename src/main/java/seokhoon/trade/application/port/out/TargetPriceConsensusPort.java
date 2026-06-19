package seokhoon.trade.application.port.out;
import seokhoon.trade.domain.research.TargetPriceConsensusSnapshot;
import java.util.*;
public interface TargetPriceConsensusPort {
    TargetPriceConsensusSnapshot save(TargetPriceConsensusSnapshot value);
    List<TargetPriceConsensusSnapshot> findByStockCode(String stockCode);
    Optional<TargetPriceConsensusSnapshot> findLatest(String stockCode);
    List<TargetPriceConsensusSnapshot> findAllTargetPrices();
    static TargetPriceConsensusPort noop(){return new TargetPriceConsensusPort(){public TargetPriceConsensusSnapshot save(TargetPriceConsensusSnapshot v){return v;}public List<TargetPriceConsensusSnapshot> findByStockCode(String c){return List.of();}public Optional<TargetPriceConsensusSnapshot> findLatest(String c){return Optional.empty();}public List<TargetPriceConsensusSnapshot> findAllTargetPrices(){return List.of();}};}
}
