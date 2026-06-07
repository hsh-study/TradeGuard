package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.indicator.IndicatorSnapshot;

import java.time.LocalDate;
import java.util.List;

public interface LoadIndicatorSnapshotsUseCase {
    List<IndicatorSnapshot> load(String stockCode, LocalDate from, LocalDate to);
}
