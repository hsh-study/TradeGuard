package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.ReplayBacktestPort;
import seokhoon.trade.domain.research.ReplayBacktestResult;
import seokhoon.trade.domain.research.ReplayBacktestRun;

import java.util.List;
import java.util.Optional;

@Component
public class ReplayBacktestPersistenceAdapter implements ReplayBacktestPort {
    private final ReplayBacktestRunJpaRepository runRepository;
    private final ReplayBacktestResultJpaRepository resultRepository;

    public ReplayBacktestPersistenceAdapter(ReplayBacktestRunJpaRepository runRepository,
                                            ReplayBacktestResultJpaRepository resultRepository) {
        this.runRepository = runRepository; this.resultRepository = resultRepository;
    }

    @Override @Transactional
    public ReplayBacktestRun saveRun(ReplayBacktestRun run) {
        ReplayBacktestRunEntity entity = run.id() == null
                ? ReplayBacktestRunEntity.from(run)
                : runRepository.findById(run.id()).orElseThrow(() -> new IllegalArgumentException("Replay run not found: " + run.id()));
        if (run.id() != null) entity.update(run);
        return runRepository.saveAndFlush(entity).toDomain();
    }

    @Override @Transactional
    public List<ReplayBacktestResult> saveResults(List<ReplayBacktestResult> results) {
        return resultRepository.saveAll(results.stream().map(ReplayBacktestResultEntity::from).toList())
                .stream().map(ReplayBacktestResultEntity::toDomain).toList();
    }

    @Override @Transactional(readOnly = true)
    public Optional<ReplayBacktestRun> findRun(long runId) {
        return runRepository.findById(runId).map(ReplayBacktestRunEntity::toDomain);
    }

    @Override @Transactional(readOnly = true)
    public List<ReplayBacktestResult> findResults(long runId) {
        return resultRepository.findByRunIdOrderByTradeDateAscCandidateRankAsc(runId).stream()
                .map(ReplayBacktestResultEntity::toDomain).toList();
    }
}
