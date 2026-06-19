package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.PaperTradingReportPort;
import seokhoon.trade.domain.research.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class PaperTradingReportPersistenceAdapter implements PaperTradingReportPort {
    private final PaperTradingReportRunJpaRepository runs;
    private final PaperTradingReportResultJpaRepository results;
    public PaperTradingReportPersistenceAdapter(PaperTradingReportRunJpaRepository runs, PaperTradingReportResultJpaRepository results) {
        this.runs = runs; this.results = results;
    }
    @Override @Transactional public PaperTradingReportRun saveRun(PaperTradingReportRun run) {
        PaperTradingReportRunEntity entity = run.id() == null ? PaperTradingReportRunEntity.from(run)
                : runs.findById(run.id()).orElseThrow(() -> new IllegalArgumentException("Paper trading report run not found: " + run.id()));
        if (run.id() != null) entity.update(run);
        return runs.saveAndFlush(entity).toDomain();
    }
    @Override @Transactional public List<PaperTradingReportResult> saveResults(List<PaperTradingReportResult> values) {
        return results.saveAll(values.stream().map(PaperTradingReportResultEntity::from).toList()).stream()
                .map(PaperTradingReportResultEntity::toDomain).toList();
    }
    @Override @Transactional(readOnly=true) public Optional<PaperTradingReportRun> findRun(long id) { return runs.findById(id).map(PaperTradingReportRunEntity::toDomain); }
    @Override @Transactional(readOnly=true) public Optional<PaperTradingReportRun> findLatestRun(LocalDate date) { return runs.findFirstByTradeDateOrderByCreatedAtDescIdDesc(date).map(PaperTradingReportRunEntity::toDomain); }
    @Override @Transactional(readOnly=true) public List<PaperTradingReportResult> findResults(long id) {
        return results.findByRunIdOrderByStrategyAscCandidateRankAsc(id).stream().map(PaperTradingReportResultEntity::toDomain).toList();
    }
}
