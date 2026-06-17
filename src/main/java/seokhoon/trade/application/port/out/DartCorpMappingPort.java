package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.DartCorpMapping;

import java.util.List;
import java.util.Optional;

public interface DartCorpMappingPort {
    DartCorpMapping save(DartCorpMapping value);
    Optional<DartCorpMapping> findByStockCode(String stockCode);
    List<DartCorpMapping> findAll();
}
