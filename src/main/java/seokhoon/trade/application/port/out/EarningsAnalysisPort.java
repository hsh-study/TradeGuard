package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.EarningsAnalysisSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EarningsAnalysisPort {
    EarningsAnalysisSnapshot save(EarningsAnalysisSnapshot value);
    Optional<EarningsAnalysisSnapshot> findByStockCodeAndBaseDate(String stockCode, LocalDate baseDate);
    Optional<EarningsAnalysisSnapshot> findLatestByStockCode(String stockCode);
    List<EarningsAnalysisSnapshot> findByBaseDate(LocalDate baseDate);
}
