package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.CatalystEvidence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CatalystEvidencePort {
    CatalystEvidence save(CatalystEvidence value);
    Optional<CatalystEvidence> findEvidenceById(long id);
    List<CatalystEvidence> findByCatalystId(long catalystId);
    List<CatalystEvidence> findEvidenceByStockCode(String stockCode);
    List<CatalystEvidence> findRecent(int limit);
    Optional<CatalystEvidence> findDuplicate(String stockCode, String title, Instant sourcePublishedAt, String sourceName);
    default Optional<CatalystEvidence> findByStockCodeAndReceiptNo(String stockCode, String receiptNo) {
        return Optional.empty();
    }
}
