package seokhoon.trade.adapter.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.research.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class ResearchPersistenceAdapter implements InvestmentThesisPort,
        InvestmentCatalystPort, MorningNotePort, QuarterlyFinancialPort,
        ValuationSnapshotPort, EarningsAnalysisPort, EarningsEventPort,
        EarningsPreviewPort, PostEarningsReviewPort, DartCorpMappingPort,
        DartFinancialImportHistoryPort, SharesOutstandingSnapshotPort {
    private final InvestmentThesisJpaRepository theses;
    private final InvestmentCatalystJpaRepository catalysts;
    private final MorningNoteJpaRepository notes;
    private final QuarterlyFinancialJpaRepository financials;
    private final ValuationSnapshotJpaRepository valuations;
    private final SharesOutstandingSnapshotJpaRepository sharesOutstandingSnapshots;
    private final EarningsAnalysisSnapshotJpaRepository earningsAnalyses;
    private final EarningsEventJpaRepository earningsEvents;
    private final EarningsPreviewJpaRepository earningsPreviews;
    private final PostEarningsReviewJpaRepository postEarningsReviews;
    private final DartCorpMappingJpaRepository dartCorpMappings;
    private final DartFinancialImportHistoryJpaRepository dartImportHistories;

    public ResearchPersistenceAdapter(
            InvestmentThesisJpaRepository theses,
            InvestmentCatalystJpaRepository catalysts,
            MorningNoteJpaRepository notes,
            QuarterlyFinancialJpaRepository financials,
            ValuationSnapshotJpaRepository valuations,
            SharesOutstandingSnapshotJpaRepository sharesOutstandingSnapshots,
            EarningsAnalysisSnapshotJpaRepository earningsAnalyses,
            EarningsEventJpaRepository earningsEvents,
            EarningsPreviewJpaRepository earningsPreviews,
            PostEarningsReviewJpaRepository postEarningsReviews,
            DartCorpMappingJpaRepository dartCorpMappings,
            DartFinancialImportHistoryJpaRepository dartImportHistories
    ) {
        this.theses = theses;
        this.catalysts = catalysts;
        this.notes = notes;
        this.financials = financials;
        this.valuations = valuations;
        this.sharesOutstandingSnapshots = sharesOutstandingSnapshots;
        this.earningsAnalyses = earningsAnalyses;
        this.earningsEvents = earningsEvents;
        this.earningsPreviews = earningsPreviews;
        this.postEarningsReviews = postEarningsReviews;
        this.dartCorpMappings = dartCorpMappings;
        this.dartImportHistories = dartImportHistories;
    }

    @Override
    @Transactional
    public InvestmentThesis save(InvestmentThesis value) {
        InvestmentThesisEntity entity = value.id() == null
                ? InvestmentThesisEntity.from(value)
                : theses.findById(value.id()).orElseGet(() -> InvestmentThesisEntity.from(value));
        entity.update(value);
        return theses.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InvestmentThesis> findThesisById(long id) {
        return theses.findById(id).map(InvestmentThesisEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvestmentThesis> find(String stockCode, ThesisStatus status) {
        Specification<InvestmentThesisEntity> specification = (root, query, cb) -> cb.conjunction();
        if (stockCode != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("stockCode"), stockCode));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return theses.findAll(specification, Sort.by(Sort.Order.desc("updatedAt")))
                .stream().map(InvestmentThesisEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public InvestmentCatalyst save(InvestmentCatalyst value) {
        InvestmentCatalystEntity entity = value.id() == null
                ? InvestmentCatalystEntity.from(value)
                : catalysts.findById(value.id()).orElseGet(() -> InvestmentCatalystEntity.from(value));
        entity.update(value);
        return catalysts.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InvestmentCatalyst> findCatalystById(long id) {
        return catalysts.findById(id).map(InvestmentCatalystEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvestmentCatalyst> find(
            String stockCode,
            LocalDate from,
            LocalDate to,
            CatalystStatus status
    ) {
        Specification<InvestmentCatalystEntity> specification = (root, query, cb) -> cb.conjunction();
        if (stockCode != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("stockCode"), stockCode));
        }
        if (from != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("expectedDate"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("expectedDate"), to));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return catalysts.findAll(specification,
                        Sort.by(Sort.Order.asc("expectedDate"), Sort.Order.desc("importance")))
                .stream().map(InvestmentCatalystEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public MorningNote save(MorningNote value) {
        MorningNoteEntity entity = notes.findByTradeDate(value.tradeDate())
                .orElseGet(() -> MorningNoteEntity.from(value));
        entity.update(value);
        return notes.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MorningNote> findByTradeDate(LocalDate tradeDate) {
        return notes.findByTradeDate(tradeDate).map(MorningNoteEntity::toDomain);
    }

    @Override
    @Transactional
    public List<QuarterlyFinancial> saveAll(List<QuarterlyFinancial> values) {
        return values.stream()
                .map(value -> {
                    QuarterlyFinancialEntity entity = financials
                            .findByStockCodeAndFiscalYearAndFiscalQuarter(
                                    value.stockCode(), value.fiscalYear(), value.fiscalQuarter())
                            .orElseGet(() -> QuarterlyFinancialEntity.from(value));
                    entity.update(value);
                    return financials.save(entity).toDomain();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuarterlyFinancial> findRecentQuarters(String stockCode, int limit) {
        return financials.findByStockCode(stockCode, PageRequest.of(
                        0,
                        limit,
                        Sort.by(Sort.Order.desc("fiscalYear"), Sort.Order.desc("fiscalQuarter"))
                ))
                .stream().map(QuarterlyFinancialEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuarterlyFinancial> findByStockCodeAndQuarter(
            String stockCode,
            int fiscalYear,
            int fiscalQuarter
    ) {
        return financials.findByStockCodeAndFiscalYearAndFiscalQuarter(stockCode, fiscalYear, fiscalQuarter)
                .map(QuarterlyFinancialEntity::toDomain);
    }

    @Override
    @Transactional
    public ValuationSnapshot save(ValuationSnapshot value) {
        ValuationSnapshotEntity entity = valuations.findByStockCodeAndTradeDate(
                        value.stockCode(), value.tradeDate())
                .orElseGet(() -> ValuationSnapshotEntity.from(value));
        entity.update(value);
        return valuations.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ValuationSnapshot> findLatestByStockCode(String stockCode, LocalDate baseDate) {
        return valuations.findFirstByStockCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(stockCode, baseDate)
                .map(ValuationSnapshotEntity::toDomain);
    }

    @Override
    @Transactional
    public SharesOutstandingSnapshot save(SharesOutstandingSnapshot value) {
        SharesOutstandingSnapshotEntity entity = sharesOutstandingSnapshots
                .findByStockCodeAndBaseDate(value.stockCode(), value.baseDate())
                .orElseGet(() -> SharesOutstandingSnapshotEntity.from(value));
        entity.update(value);
        return sharesOutstandingSnapshots.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SharesOutstandingSnapshot> findLatestSharesByStockCode(String stockCode, LocalDate baseDate) {
        return sharesOutstandingSnapshots
                .findFirstByStockCodeAndBaseDateLessThanEqualOrderByBaseDateDesc(stockCode, baseDate)
                .map(SharesOutstandingSnapshotEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SharesOutstandingSnapshot> findSharesByStockCode(String stockCode) {
        return sharesOutstandingSnapshots.findByStockCode(stockCode, Sort.by(Sort.Order.desc("baseDate")))
                .stream()
                .map(SharesOutstandingSnapshotEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public EarningsAnalysisSnapshot save(EarningsAnalysisSnapshot value) {
        EarningsAnalysisSnapshotEntity entity = earningsAnalyses.findByStockCodeAndBaseDate(
                        value.stockCode(), value.baseDate())
                .orElseGet(() -> EarningsAnalysisSnapshotEntity.from(value));
        entity.update(value);
        return earningsAnalyses.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EarningsAnalysisSnapshot> findByStockCodeAndBaseDate(String stockCode, LocalDate baseDate) {
        return earningsAnalyses.findByStockCodeAndBaseDate(stockCode, baseDate)
                .map(EarningsAnalysisSnapshotEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EarningsAnalysisSnapshot> findLatestByStockCode(String stockCode) {
        return earningsAnalyses.findFirstByStockCodeOrderByBaseDateDesc(stockCode)
                .map(EarningsAnalysisSnapshotEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarningsAnalysisSnapshot> findByBaseDate(LocalDate baseDate) {
        return earningsAnalyses.findByBaseDate(baseDate).stream()
                .map(EarningsAnalysisSnapshotEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public EarningsEvent save(EarningsEvent value) {
        EarningsEventEntity entity = value.id() == null
                ? earningsEvents.findByStockCodeAndFiscalYearAndFiscalQuarter(
                                value.stockCode(), value.fiscalYear(), value.fiscalQuarter())
                        .orElseGet(() -> EarningsEventEntity.from(value))
                : earningsEvents.findById(value.id()).orElseGet(() -> EarningsEventEntity.from(value));
        entity.update(value);
        return earningsEvents.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EarningsEvent> findById(long id) {
        return earningsEvents.findById(id).map(EarningsEventEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EarningsEvent> findEventByStockCodeAndQuarter(String stockCode, int fiscalYear, int fiscalQuarter) {
        return earningsEvents.findByStockCodeAndFiscalYearAndFiscalQuarter(stockCode, fiscalYear, fiscalQuarter)
                .map(EarningsEventEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarningsEvent> find(String stockCode, LocalDate from, LocalDate to) {
        Specification<EarningsEventEntity> specification = (root, query, cb) -> cb.conjunction();
        if (stockCode != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("stockCode"), stockCode));
        }
        if (from != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("expectedAnnouncementDate"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("expectedAnnouncementDate"), to));
        }
        return earningsEvents.findAll(specification,
                        Sort.by(Sort.Order.asc("expectedAnnouncementDate"), Sort.Order.asc("stockCode")))
                .stream().map(EarningsEventEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarningsEvent> findByStatusAndExpectedAnnouncementDateBetween(
            EarningsEventStatus status,
            LocalDate from,
            LocalDate to
    ) {
        return earningsEvents.findByStatusAndExpectedAnnouncementDateBetween(
                        status, from, to, Sort.by(Sort.Order.asc("expectedAnnouncementDate")))
                .stream().map(EarningsEventEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public EarningsPreview save(EarningsPreview value) {
        EarningsPreviewEntity entity = value.id() == null
                ? EarningsPreviewEntity.from(value)
                : earningsPreviews.findById(value.id()).orElseGet(() -> EarningsPreviewEntity.from(value));
        entity.update(value);
        return earningsPreviews.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EarningsPreview> findPreviewById(long id) {
        return earningsPreviews.findById(id).map(EarningsPreviewEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EarningsPreview> findLatestByEarningsEventId(long earningsEventId) {
        return earningsPreviews.findFirstByEarningsEventIdOrderByPreviewDateDesc(earningsEventId)
                .map(EarningsPreviewEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarningsPreview> findPreviewsByStockCode(String stockCode) {
        return earningsPreviews.findByStockCode(stockCode, Sort.by(Sort.Order.desc("previewDate")))
                .stream().map(EarningsPreviewEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarningsPreview> findByStatusAndPreviewDateBetween(
            EarningsPreviewStatus status,
            LocalDate from,
            LocalDate to
    ) {
        return earningsPreviews.findByStatusAndPreviewDateBetween(
                        status, from, to, Sort.by(Sort.Order.asc("previewDate")))
                .stream().map(EarningsPreviewEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public PostEarningsReview save(PostEarningsReview value) {
        PostEarningsReviewEntity entity = postEarningsReviews.findByEarningsEventId(value.earningsEventId())
                .orElseGet(() -> PostEarningsReviewEntity.from(value));
        entity.update(value);
        return postEarningsReviews.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PostEarningsReview> findByEarningsEventId(long earningsEventId) {
        return postEarningsReviews.findByEarningsEventId(earningsEventId)
                .map(PostEarningsReviewEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostEarningsReview> findReviewsByStockCode(String stockCode) {
        return postEarningsReviews.findByStockCode(stockCode, Sort.by(Sort.Order.desc("reviewDate")))
                .stream().map(PostEarningsReviewEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostEarningsReview> findByReviewDateBetween(LocalDate from, LocalDate to) {
        return postEarningsReviews.findByReviewDateBetween(from, to, Sort.by(Sort.Order.desc("reviewDate")))
                .stream().map(PostEarningsReviewEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostEarningsReview> findByThesisImpactIn(List<ThesisImpact> thesisImpacts) {
        return postEarningsReviews.findByThesisImpactIn(thesisImpacts, Sort.by(Sort.Order.desc("reviewDate")))
                .stream().map(PostEarningsReviewEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public DartCorpMapping save(DartCorpMapping value) {
        DartCorpMappingEntity entity = dartCorpMappings.findByStockCode(value.stockCode())
                .or(() -> dartCorpMappings.findByCorpCode(value.corpCode()))
                .orElseGet(() -> DartCorpMappingEntity.from(value));
        entity.update(value);
        return dartCorpMappings.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DartCorpMapping> findByStockCode(String stockCode) {
        return dartCorpMappings.findByStockCode(stockCode)
                .map(DartCorpMappingEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DartCorpMapping> findAll() {
        return dartCorpMappings.findAll(Sort.by(Sort.Order.asc("stockCode")))
                .stream().map(DartCorpMappingEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public DartFinancialImportHistory save(DartFinancialImportHistory value) {
        return dartImportHistories.save(DartFinancialImportHistoryEntity.from(value)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DartFinancialImportHistory> findHistoriesByStockCode(String stockCode) {
        return dartImportHistories.findByStockCode(stockCode, Sort.by(Sort.Order.desc("requestedAt")))
                .stream().map(DartFinancialImportHistoryEntity::toDomain).toList();
    }
}
