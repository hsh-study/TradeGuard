package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface InvestmentThesisJpaRepository extends JpaRepository<InvestmentThesisEntity, Long>,
        JpaSpecificationExecutor<InvestmentThesisEntity> {
}

interface InvestmentCatalystJpaRepository extends JpaRepository<InvestmentCatalystEntity, Long>,
        JpaSpecificationExecutor<InvestmentCatalystEntity> {
}

interface CatalystEvidenceJpaRepository extends JpaRepository<CatalystEvidenceEntity, Long> {
    List<CatalystEvidenceEntity> findByCatalystIdAndStatus(
            Long catalystId,
            seokhoon.trade.domain.research.EvidenceStatus status,
            Sort sort
    );

    List<CatalystEvidenceEntity> findByStockCodeAndStatus(
            String stockCode,
            seokhoon.trade.domain.research.EvidenceStatus status,
            Sort sort
    );

    List<CatalystEvidenceEntity> findByStatus(
            seokhoon.trade.domain.research.EvidenceStatus status,
            org.springframework.data.domain.Pageable pageable
    );

    Optional<CatalystEvidenceEntity> findFirstByStockCodeAndTitleAndSourcePublishedAtAndSourceNameAndStatus(
            String stockCode,
            String title,
            java.time.Instant sourcePublishedAt,
            String sourceName,
            seokhoon.trade.domain.research.EvidenceStatus status
    );

    Optional<CatalystEvidenceEntity> findFirstByStockCodeAndReceiptNoAndStatus(
            String stockCode,
            String receiptNo,
            seokhoon.trade.domain.research.EvidenceStatus status
    );
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

interface SharesOutstandingSnapshotJpaRepository extends JpaRepository<SharesOutstandingSnapshotEntity, Long> {
    Optional<SharesOutstandingSnapshotEntity> findByStockCodeAndBaseDate(String stockCode, LocalDate baseDate);

    Optional<SharesOutstandingSnapshotEntity> findFirstByStockCodeAndBaseDateLessThanEqualOrderByBaseDateDesc(
            String stockCode,
            LocalDate baseDate
    );

    List<SharesOutstandingSnapshotEntity> findByStockCode(String stockCode, Sort sort);
}

interface EarningsAnalysisSnapshotJpaRepository extends JpaRepository<EarningsAnalysisSnapshotEntity, Long> {
    Optional<EarningsAnalysisSnapshotEntity> findByStockCodeAndBaseDate(String stockCode, LocalDate baseDate);

    Optional<EarningsAnalysisSnapshotEntity> findFirstByStockCodeOrderByBaseDateDesc(String stockCode);

    List<EarningsAnalysisSnapshotEntity> findByBaseDate(LocalDate baseDate);
}

interface EarningsEventJpaRepository extends JpaRepository<EarningsEventEntity, Long>,
        JpaSpecificationExecutor<EarningsEventEntity> {
    Optional<EarningsEventEntity> findByStockCodeAndFiscalYearAndFiscalQuarter(
            String stockCode,
            int fiscalYear,
            int fiscalQuarter
    );

    List<EarningsEventEntity> findByStatusAndExpectedAnnouncementDateBetween(
            seokhoon.trade.domain.research.EarningsEventStatus status,
            LocalDate from,
            LocalDate to,
            Sort sort
    );
}

interface EarningsPreviewJpaRepository extends JpaRepository<EarningsPreviewEntity, Long> {
    Optional<EarningsPreviewEntity> findFirstByEarningsEventIdOrderByPreviewDateDesc(long earningsEventId);
    List<EarningsPreviewEntity> findByStockCode(String stockCode, Sort sort);
    List<EarningsPreviewEntity> findByStatusAndPreviewDateBetween(
            seokhoon.trade.domain.research.EarningsPreviewStatus status,
            LocalDate from,
            LocalDate to,
            Sort sort
    );
}

interface PostEarningsReviewJpaRepository extends JpaRepository<PostEarningsReviewEntity, Long> {
    Optional<PostEarningsReviewEntity> findByEarningsEventId(long earningsEventId);
    List<PostEarningsReviewEntity> findByStockCode(String stockCode, Sort sort);
    List<PostEarningsReviewEntity> findByReviewDateBetween(LocalDate from, LocalDate to, Sort sort);
    List<PostEarningsReviewEntity> findByThesisImpactIn(
            List<seokhoon.trade.domain.research.ThesisImpact> thesisImpacts,
            Sort sort
    );
}

interface DartCorpMappingJpaRepository extends JpaRepository<DartCorpMappingEntity, Long> {
    Optional<DartCorpMappingEntity> findByStockCode(String stockCode);
    Optional<DartCorpMappingEntity> findByCorpCode(String corpCode);
}

interface DartFinancialImportHistoryJpaRepository extends JpaRepository<DartFinancialImportHistoryEntity, Long> {
    List<DartFinancialImportHistoryEntity> findByStockCode(String stockCode, Sort sort);
}

interface DartCorpCodeImportHistoryJpaRepository extends JpaRepository<DartCorpCodeImportHistoryEntity, Long> {
    List<DartCorpCodeImportHistoryEntity> findAllBy(Sort sort);
}

interface SharesOutstandingImportHistoryJpaRepository extends JpaRepository<SharesOutstandingImportHistoryEntity, Long> {
    List<SharesOutstandingImportHistoryEntity> findAllBy(Sort sort);
}

interface DisclosureEvidenceImportHistoryJpaRepository
        extends JpaRepository<DisclosureEvidenceImportHistoryEntity, Long> {
    List<DisclosureEvidenceImportHistoryEntity> findAllBy(
            org.springframework.data.domain.Pageable pageable
    );
}

interface MarketIndexImportHistoryJpaRepository extends JpaRepository<MarketIndexImportHistoryEntity, Long> {
    List<MarketIndexImportHistoryEntity> findAllBy(org.springframework.data.domain.Pageable pageable);
}

interface SectorImportHistoryJpaRepository extends JpaRepository<SectorImportHistoryEntity, Long> {
    List<SectorImportHistoryEntity> findAllBy(org.springframework.data.domain.Pageable pageable);
}
