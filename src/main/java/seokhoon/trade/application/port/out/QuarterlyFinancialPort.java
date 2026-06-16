package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.QuarterlyFinancial;

import java.util.List;
import java.util.Optional;

public interface QuarterlyFinancialPort {
    List<QuarterlyFinancial> saveAll(List<QuarterlyFinancial> values);
    List<QuarterlyFinancial> findRecentQuarters(String stockCode, int limit);
    Optional<QuarterlyFinancial> findByStockCodeAndQuarter(String stockCode, int fiscalYear, int fiscalQuarter);
}
