package seokhoon.trade.adapter.persistence;

import org.springframework.data.domain.Sort;
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
        InvestmentCatalystPort, MorningNotePort {
    private final InvestmentThesisJpaRepository theses;
    private final InvestmentCatalystJpaRepository catalysts;
    private final MorningNoteJpaRepository notes;

    public ResearchPersistenceAdapter(
            InvestmentThesisJpaRepository theses,
            InvestmentCatalystJpaRepository catalysts,
            MorningNoteJpaRepository notes
    ) {
        this.theses = theses;
        this.catalysts = catalysts;
        this.notes = notes;
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
}
