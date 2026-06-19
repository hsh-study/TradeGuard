package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ResearchUseCases.CreateEvidenceCommand;
import seokhoon.trade.application.port.in.ResearchUseCases.UpdateEvidenceCommand;
import seokhoon.trade.application.port.out.CatalystEvidencePort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.research.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CatalystEvidenceServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Test
    void createsQueriesUpdatesAndLogicallyDeletesEvidence() {
        InMemoryEvidencePort port = new InMemoryEvidencePort();
        CatalystEvidenceService service = service(port);

        CatalystEvidence created = service.create(new CreateEvidenceCommand(
                1L, "005930", CatalystEvidenceType.MANUAL_NOTE, "공시 확인",
                "요약", "DART", "https://example.test/disclosure", NOW,
                EvidenceConfidence.HIGH, EvidenceCreatedBy.USER));

        assertThat(service.findByCatalystId(1L)).containsExactly(created);
        assertThat(service.findByStockCode("005930")).containsExactly(created);

        CatalystEvidence updated = service.update(created.id(), new UpdateEvidenceCommand(
                null, null, null, null, "수정 요약", null, null, null, EvidenceConfidence.MEDIUM));

        assertThat(updated.summary()).isEqualTo("수정 요약");
        assertThat(updated.confidence()).isEqualTo(EvidenceConfidence.MEDIUM);

        service.delete(created.id());

        assertThat(service.findByCatalystId(1L)).isEmpty();
    }

    @Test
    void preventsDuplicateEvidenceByLogicalKey() {
        InMemoryEvidencePort port = new InMemoryEvidencePort();
        CatalystEvidenceService service = service(port);
        CreateEvidenceCommand command = new CreateEvidenceCommand(
                null, "005930", CatalystEvidenceType.DART_DISCLOSURE, "공시",
                "요약", "DART", "https://example.test/a", NOW,
                EvidenceConfidence.HIGH, EvidenceCreatedBy.PROVIDER);

        CatalystEvidence first = service.create(command);
        CatalystEvidence second = service.create(command);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(port.values).hasSize(1);
    }

    private static CatalystEvidenceService service(InMemoryEvidencePort port) {
        return new CatalystEvidenceService(port, OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    static class InMemoryEvidencePort implements CatalystEvidencePort {
        private final List<CatalystEvidence> values = new ArrayList<>();

        @Override
        public CatalystEvidence save(CatalystEvidence value) {
            CatalystEvidence saved = new CatalystEvidence(
                    value.id() == null ? (long) values.size() + 1 : value.id(),
                    value.catalystId(), value.stockCode(), value.evidenceType(),
                    value.title(), value.summary(), value.sourceName(), value.sourceUrl(),
                    value.sourcePublishedAt(), value.receiptNo(), value.disclosureType(),
                    value.relatedCatalystType(), value.importance(), value.rawCategory(),
                    value.confidence(), value.createdBy(),
                    value.status(), value.createdAt(), value.updatedAt());
            values.removeIf(existing -> existing.id().equals(saved.id()));
            values.add(saved);
            return saved;
        }

        @Override public Optional<CatalystEvidence> findEvidenceById(long id) {
            return values.stream().filter(value -> value.id() == id).findFirst();
        }
        @Override public List<CatalystEvidence> findByCatalystId(long catalystId) {
            return values.stream()
                    .filter(value -> value.status() == EvidenceStatus.ACTIVE)
                    .filter(value -> value.catalystId() != null && value.catalystId() == catalystId)
                    .toList();
        }
        @Override public List<CatalystEvidence> findEvidenceByStockCode(String stockCode) {
            return values.stream()
                    .filter(value -> value.status() == EvidenceStatus.ACTIVE)
                    .filter(value -> stockCode.equals(value.stockCode()))
                    .toList();
        }
        @Override public List<CatalystEvidence> findRecent(int limit) {
            return values.stream()
                    .filter(value -> value.status() == EvidenceStatus.ACTIVE)
                    .sorted(Comparator.comparing(CatalystEvidence::createdAt).reversed())
                    .limit(limit)
                    .toList();
        }
        @Override
        public Optional<CatalystEvidence> findDuplicate(
                String stockCode,
                String title,
                Instant sourcePublishedAt,
                String sourceName
        ) {
            return values.stream()
                    .filter(value -> value.status() == EvidenceStatus.ACTIVE)
                    .filter(value -> java.util.Objects.equals(value.stockCode(), stockCode))
                    .filter(value -> java.util.Objects.equals(value.title(), title))
                    .filter(value -> java.util.Objects.equals(value.sourcePublishedAt(), sourcePublishedAt))
                    .filter(value -> java.util.Objects.equals(value.sourceName(), sourceName))
                    .findFirst();
        }
        @Override public Optional<CatalystEvidence> findByStockCodeAndReceiptNo(String stockCode,String receiptNo) {
            return values.stream().filter(value->java.util.Objects.equals(stockCode,value.stockCode()))
                    .filter(value->java.util.Objects.equals(receiptNo,value.receiptNo())).findFirst();
        }
    }
}
