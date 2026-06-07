package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.indicator.IndicatorSnapshot;

import java.time.LocalDate;
import java.util.List;

public interface IndicatorSnapshotPort {
    IndicatorSnapshot save(IndicatorSnapshot snapshot);

    List<IndicatorSnapshot> findByStockCodeAndTradeDateBetween(String stockCode, LocalDate from, LocalDate to);
}
