package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.EarlyMarketAfterHoursSnapshotPort;
import seokhoon.trade.application.port.out.EarlyMarketDataCapturePort;
import seokhoon.trade.application.port.out.EarlyMarketIntradayBarSnapshotPort;
import seokhoon.trade.application.port.out.EarlyMarketMarketSnapshotArchivePort;
import seokhoon.trade.application.port.out.EarlyMarketRankingSnapshotPort;
import seokhoon.trade.domain.market.EarlyMarketAfterHoursSnapshot;
import seokhoon.trade.domain.market.EarlyMarketDataCapture;
import seokhoon.trade.domain.market.EarlyMarketIntradayBarSnapshot;
import seokhoon.trade.domain.market.EarlyMarketMarketSnapshot;
import seokhoon.trade.domain.market.EarlyMarketRankingSnapshot;

import java.time.LocalDate;
import java.util.List;

@Component
public class EarlyMarketArchivePersistenceAdapter
        implements EarlyMarketDataCapturePort,
        EarlyMarketRankingSnapshotPort,
        EarlyMarketAfterHoursSnapshotPort,
        EarlyMarketIntradayBarSnapshotPort,
        EarlyMarketMarketSnapshotArchivePort {
    private final EarlyMarketDataCaptureJpaRepository captureRepository;
    private final EarlyMarketRankingSnapshotJpaRepository rankingRepository;
    private final EarlyMarketAfterHoursSnapshotJpaRepository afterHoursRepository;
    private final EarlyMarketIntradayBarSnapshotJpaRepository barRepository;
    private final EarlyMarketMarketSnapshotJpaRepository snapshotRepository;

    public EarlyMarketArchivePersistenceAdapter(
            EarlyMarketDataCaptureJpaRepository captureRepository,
            EarlyMarketRankingSnapshotJpaRepository rankingRepository,
            EarlyMarketAfterHoursSnapshotJpaRepository afterHoursRepository,
            EarlyMarketIntradayBarSnapshotJpaRepository barRepository,
            EarlyMarketMarketSnapshotJpaRepository snapshotRepository
    ) {
        this.captureRepository = captureRepository;
        this.rankingRepository = rankingRepository;
        this.afterHoursRepository = afterHoursRepository;
        this.barRepository = barRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    @Transactional
    public EarlyMarketDataCapture save(EarlyMarketDataCapture capture) {
        EarlyMarketDataCaptureEntity entity = captureRepository
                .findByTradeDateAndCaptureType(
                        capture.tradeDate(),
                        capture.captureType()
                )
                .map(existing -> {
                    existing.update(capture);
                    return existing;
                })
                .orElseGet(() -> EarlyMarketDataCaptureEntity.from(capture));
        return captureRepository.saveAndFlush(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyMarketDataCapture> findCaptures(LocalDate tradeDate) {
        return captureRepository
                .findByTradeDateOrderByCapturedAtAscCaptureTypeAsc(tradeDate)
                .stream()
                .map(EarlyMarketDataCaptureEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<EarlyMarketRankingSnapshot> saveAll(
            List<EarlyMarketRankingSnapshot> snapshots
    ) {
        return rankingRepository.saveAll(
                        snapshots.stream()
                                .map(EarlyMarketRankingSnapshotEntity::from)
                                .toList()
                )
                .stream()
                .map(EarlyMarketRankingSnapshotEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyMarketRankingSnapshot> findRankings(LocalDate tradeDate) {
        return rankingRepository
                .findByTradeDateOrderByCapturedAtAscSourceAscRankAsc(tradeDate)
                .stream()
                .map(EarlyMarketRankingSnapshotEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<EarlyMarketAfterHoursSnapshot> upsertAfterHours(
            List<EarlyMarketAfterHoursSnapshot> snapshots
    ) {
        return afterHoursRepository.saveAll(snapshots.stream()
                        .map(snapshot -> afterHoursRepository
                                .findByTradeDateAndPreviousTradingDayAndStockCode(
                                        snapshot.tradeDate(),
                                        snapshot.previousTradingDay(),
                                        snapshot.stockCode()
                                )
                                .map(entity -> {
                                    entity.update(snapshot);
                                    return entity;
                                })
                                .orElseGet(() ->
                                        EarlyMarketAfterHoursSnapshotEntity.from(
                                                snapshot
                                        )))
                        .toList())
                .stream()
                .map(EarlyMarketAfterHoursSnapshotEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyMarketAfterHoursSnapshot> findAfterHours(
            LocalDate tradeDate
    ) {
        return afterHoursRepository.findByTradeDateOrderByStockCodeAsc(tradeDate)
                .stream()
                .map(EarlyMarketAfterHoursSnapshotEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<EarlyMarketIntradayBarSnapshot> upsertBars(
            List<EarlyMarketIntradayBarSnapshot> snapshots
    ) {
        return barRepository.saveAll(snapshots.stream()
                        .map(snapshot -> barRepository
                                .findByTradeDateAndStockCodeAndBarTimeAndIntervalType(
                                        snapshot.tradeDate(),
                                        snapshot.stockCode(),
                                        snapshot.barTime(),
                                        snapshot.intervalType()
                                )
                                .map(entity -> {
                                    entity.update(snapshot);
                                    return entity;
                                })
                                .orElseGet(() ->
                                        EarlyMarketIntradayBarSnapshotEntity.from(
                                                snapshot
                                        )))
                        .toList())
                .stream()
                .map(EarlyMarketIntradayBarSnapshotEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyMarketIntradayBarSnapshot> findBars(
            LocalDate tradeDate,
            String stockCode
    ) {
        return barRepository
                .findByTradeDateAndStockCodeOrderByBarTimeAsc(
                        tradeDate,
                        stockCode
                )
                .stream()
                .map(EarlyMarketIntradayBarSnapshotEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<EarlyMarketMarketSnapshot> upsertMarketSnapshots(
            List<EarlyMarketMarketSnapshot> snapshots
    ) {
        return snapshotRepository.saveAll(snapshots.stream()
                        .map(snapshot -> snapshotRepository
                                .findByTradeDateAndStockCodeAndCapturedAtAndSnapshotType(
                                        snapshot.tradeDate(),
                                        snapshot.stockCode(),
                                        snapshot.capturedAt(),
                                        snapshot.snapshotType()
                                )
                                .map(entity -> {
                                    entity.update(snapshot);
                                    return entity;
                                })
                                .orElseGet(() ->
                                        EarlyMarketMarketSnapshotEntity.from(
                                                snapshot
                                        )))
                        .toList())
                .stream()
                .map(EarlyMarketMarketSnapshotEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyMarketMarketSnapshot> findMarketSnapshots(
            LocalDate tradeDate,
            String stockCode
    ) {
        return snapshotRepository
                .findByTradeDateAndStockCodeOrderByCapturedAtAscSnapshotTypeAsc(
                        tradeDate,
                        stockCode
                )
                .stream()
                .map(EarlyMarketMarketSnapshotEntity::toDomain)
                .toList();
    }
}
