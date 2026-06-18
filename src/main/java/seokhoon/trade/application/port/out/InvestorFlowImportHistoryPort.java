package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.InvestorFlowImportHistory;
import java.util.List;

public interface InvestorFlowImportHistoryPort {
    InvestorFlowImportHistory save(InvestorFlowImportHistory history);
    List<InvestorFlowImportHistory> findRecent(String stockCode, int limit);
}
