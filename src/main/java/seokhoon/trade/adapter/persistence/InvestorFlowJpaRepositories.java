package seokhoon.trade.adapter.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import seokhoon.trade.domain.market.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface StockInvestorFlowJpaRepository extends JpaRepository<StockInvestorFlowEntity, Long> {
    Optional<StockInvestorFlowEntity> findByStockCodeAndTradeDateAndInvestorTypeAndSource(
            String stockCode, LocalDate tradeDate, InvestorType investorType, InvestorFlowSource source);
    List<StockInvestorFlowEntity> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);
    List<StockInvestorFlowEntity> findByStockCodeAndTradeDateBetweenOrderByTradeDateDesc(
            String stockCode, LocalDate from, LocalDate to);
}

interface MarketInvestorFlowJpaRepository extends JpaRepository<MarketInvestorFlowEntity, Long> {
    Optional<MarketInvestorFlowEntity> findByMarketAndTradeDateAndInvestorTypeAndSource(
            InvestorFlowMarket market, LocalDate tradeDate, InvestorType investorType, InvestorFlowSource source);
    List<MarketInvestorFlowEntity> findByMarketAndTradeDate(InvestorFlowMarket market, LocalDate tradeDate);
    List<MarketInvestorFlowEntity> findByMarketAndTradeDateBetweenOrderByTradeDateDesc(
            InvestorFlowMarket market, LocalDate from, LocalDate to);
}

interface StockSupplyDemandSnapshotJpaRepository extends JpaRepository<StockSupplyDemandSnapshotEntity, Long> {
    Optional<StockSupplyDemandSnapshotEntity> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);
    Optional<StockSupplyDemandSnapshotEntity> findFirstByStockCodeOrderByTradeDateDesc(String stockCode);
    List<StockSupplyDemandSnapshotEntity> findByTradeDate(LocalDate tradeDate);
}

interface InvestorFlowImportHistoryJpaRepository extends JpaRepository<InvestorFlowImportHistoryEntity, Long> {
    List<InvestorFlowImportHistoryEntity> findByStockCode(String stockCode, Pageable pageable);
    List<InvestorFlowImportHistoryEntity> findAllBy(Pageable pageable);
}
