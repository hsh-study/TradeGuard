package seokhoon.trade.adapter.persistence;
import org.springframework.data.domain.Sort;import org.springframework.data.jpa.domain.Specification;import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;import seokhoon.trade.application.port.out.*;import seokhoon.trade.domain.research.*;import java.util.*;
@Component public class ConsensusPersistenceAdapter implements EarningsConsensusPort,TargetPriceConsensusPort {
 private final EarningsConsensusSnapshotJpaRepository earnings;private final TargetPriceConsensusSnapshotJpaRepository targets;
 public ConsensusPersistenceAdapter(EarningsConsensusSnapshotJpaRepository earnings,TargetPriceConsensusSnapshotJpaRepository targets){this.earnings=earnings;this.targets=targets;}
 @Transactional public EarningsConsensusSnapshot save(EarningsConsensusSnapshot v){return earnings.save(EarningsConsensusSnapshotEntity.from(v)).toDomain();}
 @Transactional(readOnly=true)public List<EarningsConsensusSnapshot> find(String stockCode,Integer year,Integer quarter){Specification<EarningsConsensusSnapshotEntity>s=(r,q,c)->c.equal(r.get("stockCode"),stockCode);if(year!=null)s=s.and((r,q,c)->c.equal(r.get("fiscalYear"),year));if(quarter!=null)s=s.and((r,q,c)->c.equal(r.get("fiscalQuarter"),quarter));return earnings.findAll(s,Sort.by(Sort.Order.desc("consensusDate"))).stream().map(EarningsConsensusSnapshotEntity::toDomain).toList();}
 @Transactional(readOnly=true)public Optional<EarningsConsensusSnapshot> findLatest(String stockCode,int year,int quarter){return earnings.findFirstByStockCodeAndFiscalYearAndFiscalQuarterOrderByConsensusDateDesc(stockCode,year,quarter).map(EarningsConsensusSnapshotEntity::toDomain);}
 @Transactional(readOnly=true)public List<EarningsConsensusSnapshot> findAllEarnings(){return earnings.findAll().stream().map(EarningsConsensusSnapshotEntity::toDomain).toList();}
 @Transactional public TargetPriceConsensusSnapshot save(TargetPriceConsensusSnapshot v){return targets.save(TargetPriceConsensusSnapshotEntity.from(v)).toDomain();}
 @Transactional(readOnly=true)public List<TargetPriceConsensusSnapshot> findByStockCode(String code){return targets.findByStockCode(code,Sort.by(Sort.Order.desc("consensusDate"))).stream().map(TargetPriceConsensusSnapshotEntity::toDomain).toList();}
 @Transactional(readOnly=true)public Optional<TargetPriceConsensusSnapshot> findLatest(String code){return targets.findFirstByStockCodeOrderByConsensusDateDesc(code).map(TargetPriceConsensusSnapshotEntity::toDomain);}
 @Transactional(readOnly=true)public List<TargetPriceConsensusSnapshot> findAllTargetPrices(){return targets.findAll().stream().map(TargetPriceConsensusSnapshotEntity::toDomain).toList();}
}
