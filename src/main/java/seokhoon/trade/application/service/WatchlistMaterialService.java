package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.ImportDisclosureActualEvidenceUseCase;
import seokhoon.trade.application.port.in.WatchlistMaterialUseCase;
import seokhoon.trade.application.port.in.DartCorpCodeImportUseCase;
import seokhoon.trade.application.port.out.DartCorpMappingPort;
import seokhoon.trade.application.port.out.StockPort;
import seokhoon.trade.domain.research.CatalystEvidence;
import seokhoon.trade.domain.research.DisclosureEvidenceImportHistory;
import seokhoon.trade.domain.research.DisclosureEvidenceImportStatus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class WatchlistMaterialService implements WatchlistMaterialUseCase {
    private final StockPort stockPort;
    private final ImportDisclosureActualEvidenceUseCase disclosureUseCase;
    private final DartCorpMappingPort dartCorpMappingPort;
    private final DartCorpCodeImportUseCase dartCorpCodeImportUseCase;

    public WatchlistMaterialService(
            StockPort stockPort,
            ImportDisclosureActualEvidenceUseCase disclosureUseCase,
            DartCorpMappingPort dartCorpMappingPort,
            DartCorpCodeImportUseCase dartCorpCodeImportUseCase
    ) {
        this.stockPort = stockPort;
        this.disclosureUseCase = disclosureUseCase;
        this.dartCorpMappingPort = dartCorpMappingPort;
        this.dartCorpCodeImportUseCase = dartCorpCodeImportUseCase;
    }

    @Override
    public CollectionResult collect(String stockCode, LocalDate from, LocalDate to) {
        validate(stockCode, from, to);
        requireRegistered(stockCode);
        if (dartCorpMappingPort.findByStockCode(stockCode).isEmpty()) {
            dartCorpCodeImportUseCase.importCorpCodes();
        }
        DisclosureEvidenceImportHistory history = disclosureUseCase
                .importStock(stockCode, from, to);
        return new CollectionResult(stockCode, from, to, history.status(),
                history.importedCount(), message(history.status()));
    }

    @Override
    public List<MaterialItem> find(String stockCode, LocalDate from, LocalDate to) {
        validate(stockCode, from, to);
        requireRegistered(stockCode);
        return disclosureUseCase.findEvidences(stockCode, from, to).stream()
                .sorted(Comparator.comparing(CatalystEvidence::sourcePublishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(value -> new MaterialItem(value.id(), value.evidenceType(),
                        value.title(), value.summary(), value.sourcePublishedAt(),
                        value.relatedCatalystType(), value.importance(), value.confidence()))
                .toList();
    }

    private void requireRegistered(String stockCode) {
        stockPort.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("stock not found: " + stockCode));
    }

    private static void validate(String stockCode, LocalDate from, LocalDate to) {
        if (stockCode == null || !stockCode.matches("[0-9A-Za-z]{1,12}")) {
            throw new IllegalArgumentException("invalid stockCode");
        }
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        if (from.plusDays(365).isBefore(to)) {
            throw new IllegalArgumentException("material range must be 365 days or less");
        }
    }

    private static String message(DisclosureEvidenceImportStatus status) {
        return switch (status) {
            case SUCCESS -> "COLLECTION_COMPLETED";
            case PARTIAL -> "COLLECTION_PARTIALLY_COMPLETED";
            case SKIPPED -> "DISCLOSURE_PROVIDER_DISABLED_OR_NOT_READY";
            case FAILED -> "COLLECTION_FAILED";
        };
    }
}
