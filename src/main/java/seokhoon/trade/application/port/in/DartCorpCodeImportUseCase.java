package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.DartCorpCodeImportHistory;

import java.util.List;

public interface DartCorpCodeImportUseCase {
    DartCorpCodeImportHistory importCorpCodes();
    DartCorpCodeImportHistory importCorpCodesFromFile(byte[] content);
    List<DartCorpCodeImportHistory> findCorpCodeImportHistories();
}
