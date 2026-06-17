package seokhoon.trade.application.service;

import seokhoon.trade.application.port.out.CatalystEvidencePort;
import seokhoon.trade.domain.research.CatalystEvidence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

class NoopCatalystEvidencePort implements CatalystEvidencePort {
    @Override public CatalystEvidence save(CatalystEvidence value) { return value; }
    @Override public Optional<CatalystEvidence> findEvidenceById(long id) { return Optional.empty(); }
    @Override public List<CatalystEvidence> findByCatalystId(long catalystId) { return List.of(); }
    @Override public List<CatalystEvidence> findEvidenceByStockCode(String stockCode) { return List.of(); }
    @Override public List<CatalystEvidence> findRecent(int limit) { return List.of(); }
    @Override
    public Optional<CatalystEvidence> findDuplicate(
            String stockCode,
            String title,
            Instant sourcePublishedAt,
            String sourceName
    ) {
        return Optional.empty();
    }
}
