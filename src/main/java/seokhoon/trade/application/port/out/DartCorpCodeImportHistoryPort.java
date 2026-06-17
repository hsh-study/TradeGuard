package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.DartCorpCodeImportHistory;

import java.util.List;

public interface DartCorpCodeImportHistoryPort {
    DartCorpCodeImportHistory save(DartCorpCodeImportHistory value);
    List<DartCorpCodeImportHistory> findAllCorpCodeImports();
}
