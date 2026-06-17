package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.DartFinancialStatement;

public interface DartFinancialProviderPort {
    DartFinancialStatement fetchFinancialStatement(String corpCode, int fiscalYear, String reportCode);
}
