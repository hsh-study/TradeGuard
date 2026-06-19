package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.ResearchUseCases.*;
import seokhoon.trade.application.port.out.CatalystEvidencePort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.research.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class CatalystEvidenceService implements CatalystEvidenceUseCase {
    private final CatalystEvidencePort port;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public CatalystEvidenceService(CatalystEvidencePort port, OperationalMetricsPort metrics) {
        this(port, metrics, Clock.systemUTC());
    }

    CatalystEvidenceService(CatalystEvidencePort port, OperationalMetricsPort metrics, Clock clock) {
        this.port = port;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CatalystEvidence create(CreateEvidenceCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = clock.instant();
        CatalystEvidence value = new CatalystEvidence(
                null,
                command.catalystId(),
                command.stockCode(),
                command.evidenceType() == null ? CatalystEvidenceType.MANUAL_NOTE : command.evidenceType(),
                command.title(),
                command.summary(),
                command.sourceName(),
                command.sourceUrl(),
                command.sourcePublishedAt(),
                command.confidence() == null ? EvidenceConfidence.MEDIUM : command.confidence(),
                command.createdBy() == null ? EvidenceCreatedBy.USER : command.createdBy(),
                EvidenceStatus.ACTIVE,
                now,
                now
        );
        CatalystEvidence saved = saveDeduplicated(value);
        metrics.recordCatalystEvidence(saved.evidenceType().name(), saved.confidence().name());
        return saved;
    }

    public CatalystEvidence saveSystemEvidence(
            Long catalystId,
            String stockCode,
            CatalystEvidenceType type,
            String title,
            String summary,
            String sourceName,
            String sourceUrl,
            Instant sourcePublishedAt,
            EvidenceConfidence confidence
    ) {
        Instant now = clock.instant();
        return saveDeduplicated(new CatalystEvidence(null, catalystId, stockCode, type,
                title, summary, sourceName, sourceUrl, sourcePublishedAt,
                confidence, EvidenceCreatedBy.SYSTEM, EvidenceStatus.ACTIVE, now, now));
    }

    public CatalystEvidence saveProviderEvidence(DisclosureEvidenceRecord record) {
        Instant now = clock.instant();
        return saveDeduplicated(new CatalystEvidence(null, null, record.stockCode(),
                record.evidenceType(), record.title(), record.summary(), record.sourceName(),
                record.sourceUrl(), record.sourcePublishedAt(), record.confidence(),
                EvidenceCreatedBy.PROVIDER, EvidenceStatus.ACTIVE, now, now));
    }

    public CatalystEvidence saveActualEvidence(DisclosureActualRecord record, Long catalystId) {
        if (record.receiptNo() != null && !record.receiptNo().isBlank()) {
            var duplicate = port.findByStockCodeAndReceiptNo(record.stockCode(), record.receiptNo());
            if (duplicate.isPresent()) return duplicate.get();
        }
        Instant now = clock.instant();
        Instant publishedAt = record.disclosureDate().atTime(
                record.disclosureTime() == null ? java.time.LocalTime.NOON : record.disclosureTime())
                .atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant();
        CatalystEvidence value = new CatalystEvidence(null, catalystId, record.stockCode(),
                record.source() == DisclosureProvider.KRX ? CatalystEvidenceType.KRX_DISCLOSURE
                        : CatalystEvidenceType.DART_DISCLOSURE,
                record.title(), "공시 metadata: type=" + record.disclosureType()
                        + ", importance=" + record.importance(), record.source().name(), record.sourceUrl(),
                publishedAt, record.receiptNo(), record.disclosureType(), record.relatedCatalystType(),
                record.importance(), record.rawCategory(),
                record.importance() == CatalystImportance.HIGH ? EvidenceConfidence.HIGH : EvidenceConfidence.MEDIUM,
                EvidenceCreatedBy.PROVIDER, EvidenceStatus.ACTIVE, now, now);
        return saveDeduplicated(value);
    }

    private CatalystEvidence saveDeduplicated(CatalystEvidence value) {
        return port.findDuplicate(value.stockCode(), value.title(), value.sourcePublishedAt(), value.sourceName())
                .orElseGet(() -> port.save(value));
    }

    @Override
    public List<CatalystEvidence> findByCatalystId(long catalystId) {
        return port.findByCatalystId(catalystId);
    }

    @Override
    public List<CatalystEvidence> findByStockCode(String stockCode) {
        return port.findEvidenceByStockCode(stockCode);
    }

    @Override
    @Transactional
    public CatalystEvidence update(long id, UpdateEvidenceCommand command) {
        CatalystEvidence current = port.findEvidenceById(id)
                .orElseThrow(() -> new ResearchNotFoundException("Evidence not found: " + id));
        CatalystEvidence saved = port.save(new CatalystEvidence(
                current.id(),
                command.catalystId() == null ? current.catalystId() : command.catalystId(),
                command.stockCode() == null ? current.stockCode() : command.stockCode(),
                command.evidenceType() == null ? current.evidenceType() : command.evidenceType(),
                command.title() == null ? current.title() : command.title(),
                command.summary() == null ? current.summary() : command.summary(),
                command.sourceName() == null ? current.sourceName() : command.sourceName(),
                command.sourceUrl() == null ? current.sourceUrl() : command.sourceUrl(),
                command.sourcePublishedAt() == null ? current.sourcePublishedAt() : command.sourcePublishedAt(),
                command.confidence() == null ? current.confidence() : command.confidence(),
                current.createdBy(),
                current.status(),
                current.createdAt(),
                clock.instant()
        ));
        metrics.recordCatalystEvidence(saved.evidenceType().name(), saved.confidence().name());
        return saved;
    }

    @Override
    @Transactional
    public void delete(long id) {
        CatalystEvidence current = port.findEvidenceById(id)
                .orElseThrow(() -> new ResearchNotFoundException("Evidence not found: " + id));
        port.save(new CatalystEvidence(current.id(), current.catalystId(), current.stockCode(),
                current.evidenceType(), current.title(), current.summary(), current.sourceName(),
                current.sourceUrl(), current.sourcePublishedAt(), current.confidence(),
                current.createdBy(), EvidenceStatus.DELETED, current.createdAt(), clock.instant()));
    }
}
