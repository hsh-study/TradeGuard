package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.indicator.IndicatorSnapshot;

public interface SaveIndicatorSnapshotUseCase {
    IndicatorSnapshot save(IndicatorSnapshot snapshot);
}
