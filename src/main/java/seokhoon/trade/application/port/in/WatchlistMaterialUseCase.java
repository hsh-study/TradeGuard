package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.CatalystEvidenceType;
import seokhoon.trade.domain.research.CatalystImportance;
import seokhoon.trade.domain.research.CatalystType;
import seokhoon.trade.domain.research.DisclosureEvidenceImportStatus;
import seokhoon.trade.domain.research.EvidenceConfidence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface WatchlistMaterialUseCase {
    CollectionResult collect(String stockCode, LocalDate from, LocalDate to);

    List<MaterialItem> find(String stockCode, LocalDate from, LocalDate to);

    record CollectionResult(
            String stockCode,
            LocalDate from,
            LocalDate to,
            DisclosureEvidenceImportStatus status,
            int importedCount,
            String message
    ) {
    }

    record MaterialItem(
            Long id,
            CatalystEvidenceType evidenceType,
            String title,
            String summary,
            Instant publishedAt,
            CatalystType catalystType,
            CatalystImportance importance,
            EvidenceConfidence confidence
    ) {
    }
}
