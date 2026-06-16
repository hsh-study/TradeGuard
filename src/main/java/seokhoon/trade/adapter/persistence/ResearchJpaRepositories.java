package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface InvestmentThesisJpaRepository extends JpaRepository<InvestmentThesisEntity, Long>,
        JpaSpecificationExecutor<InvestmentThesisEntity> {
}

interface InvestmentCatalystJpaRepository extends JpaRepository<InvestmentCatalystEntity, Long>,
        JpaSpecificationExecutor<InvestmentCatalystEntity> {
}

interface MorningNoteJpaRepository extends JpaRepository<MorningNoteEntity, Long> {
    Optional<MorningNoteEntity> findByTradeDate(LocalDate tradeDate);
}

interface QuarterlyFinancialJpaRepository extends JpaRepository<QuarterlyFinancialEntity, Long> {
    Optional<QuarterlyFinancialEntity> findByStockCodeAndFiscalYearAndFiscalQuarter(
            String stockCode,
            int fiscalYear,
            int fiscalQuarter
    );

    List<QuarterlyFinancialEntity> findByStockCode(
            String stockCode,
            org.springframework.data.domain.Pageable pageable
    );
}

interface ValuationSnapshotJpaRepository extends JpaRepository<ValuationSnapshotEntity, Long> {
    Optional<ValuationSnapshotEntity> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

    Optional<ValuationSnapshotEntity> findFirstByStockCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            String stockCode,
            LocalDate tradeDate
    );
}

interface EarningsAnalysisSnapshotJpaRepository extends JpaRepository<EarningsAnalysisSnapshotEntity, Long> {
    Optional<EarningsAnalysisSnapshotEntity> findByStockCodeAndBaseDate(String stockCode, LocalDate baseDate);

    Optional<EarningsAnalysisSnapshotEntity> findFirstByStockCodeOrderByBaseDateDesc(String stockCode);

    List<EarningsAnalysisSnapshotEntity> findByBaseDate(LocalDate baseDate);
}
