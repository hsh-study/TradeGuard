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
        ValuationSnapshotPort, EarningsAnalysisPort {
    private final InvestmentThesisJpaRepository theses;
    private final InvestmentCatalystJpaRepository catalysts;
    private final MorningNoteJpaRepository notes;
    private final QuarterlyFinancialJpaRepository financials;
    private final ValuationSnapshotJpaRepository valuations;
    private final EarningsAnalysisSnapshotJpaRepository earningsAnalyses;

    public ResearchPersistenceAdapter(
            InvestmentThesisJpaRepository theses,
            InvestmentCatalystJpaRepository catalysts,
            MorningNoteJpaRepository notes,
            QuarterlyFinancialJpaRepository financials,
            ValuationSnapshotJpaRepository valuations,
            EarningsAnalysisSnapshotJpaRepository earningsAnalyses
    ) {
        this.theses = theses;
        this.catalysts = catalysts;
        this.notes = notes;
        this.financials = financials;
        this.valuations = valuations;
        this.earningsAnalyses = earningsAnalyses;
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
}
