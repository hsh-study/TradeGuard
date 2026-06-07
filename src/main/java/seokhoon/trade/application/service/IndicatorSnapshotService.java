package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.LoadIndicatorSnapshotsUseCase;
import seokhoon.trade.application.port.in.SaveIndicatorSnapshotUseCase;
import seokhoon.trade.application.port.out.IndicatorSnapshotPort;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class IndicatorSnapshotService implements SaveIndicatorSnapshotUseCase, LoadIndicatorSnapshotsUseCase {
    private final IndicatorSnapshotPort indicatorSnapshotPort;

    public IndicatorSnapshotService(IndicatorSnapshotPort indicatorSnapshotPort) {
        this.indicatorSnapshotPort = indicatorSnapshotPort;
    }

    @Override
    @Transactional
    public IndicatorSnapshot save(IndicatorSnapshot snapshot) {
        return indicatorSnapshotPort.save(Objects.requireNonNull(snapshot, "snapshot"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndicatorSnapshot> load(String stockCode, LocalDate from, LocalDate to) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("date range must not be null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        return indicatorSnapshotPort.findByStockCodeAndTradeDateBetween(stockCode, from, to);
    }
}
