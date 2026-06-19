package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.ImportDisclosureActualEvidenceUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.DisclosureActualProviderProperties;
import seokhoon.trade.domain.position.LivePosition;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.stock.Stock;

import java.time.*;
import java.util.*;

@Service
public class DisclosureActualEvidenceImportService implements ImportDisclosureActualEvidenceUseCase {
    private final DisclosureActualProviderPort provider;
    private final DisclosureEvidenceImportHistoryPort histories;
    private final CatalystEvidencePort evidences;
    private final CatalystEvidenceService evidenceService;
    private final InvestmentCatalystPort catalysts;
    private final StockPort stocks;
    private final LivePositionPort positions;
    private final DisclosureActualProviderProperties properties;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public DisclosureActualEvidenceImportService(DisclosureActualProviderPort provider,
            DisclosureEvidenceImportHistoryPort histories, CatalystEvidencePort evidences,
            CatalystEvidenceService evidenceService, InvestmentCatalystPort catalysts, StockPort stocks,
            LivePositionPort positions, DisclosureActualProviderProperties properties,
            OperationalMetricsPort metrics) {
        this(provider,histories,evidences,evidenceService,catalysts,stocks,positions,properties,metrics,Clock.systemUTC());
    }

    DisclosureActualEvidenceImportService(DisclosureActualProviderPort provider,
            DisclosureEvidenceImportHistoryPort histories, CatalystEvidencePort evidences,
            CatalystEvidenceService evidenceService, InvestmentCatalystPort catalysts, StockPort stocks,
            LivePositionPort positions, DisclosureActualProviderProperties properties,
            OperationalMetricsPort metrics, Clock clock) {
        this.provider=provider;this.histories=histories;this.evidences=evidences;this.evidenceService=evidenceService;
        this.catalysts=catalysts;this.stocks=stocks;this.positions=positions;this.properties=properties;
        this.metrics=metrics;this.clock=clock;
    }

    @Override @Transactional
    public DisclosureEvidenceImportHistory importStock(String stockCode, LocalDate from, LocalDate to) {
        Objects.requireNonNull(stockCode,"stockCode"); Objects.requireNonNull(from,"from"); Objects.requireNonNull(to,"to");
        if(from.isAfter(to))throw new IllegalArgumentException("from must not be after to");
        Instant requested=clock.instant();
        if(!properties.isEnabled())return save(stockCode,from,to,DisclosureEvidenceImportStatus.SKIPPED,0,
                "disclosure actual provider disabled",requested);
        try {
            List<DisclosureActualRecord> records=provider.fetchDisclosures(stockCode,from,to);
            int imported=0; boolean partial=false;
            for(DisclosureActualRecord record:records) {
                try {
                    if(isDuplicate(record))continue;
                    Long catalystId=relatedCatalyst(record).map(InvestmentCatalyst::id).orElse(null);
                    evidenceService.saveActualEvidence(record,catalystId); imported++;
                    metrics.recordDisclosureActualEvidence(record.importance().name().toLowerCase(Locale.ROOT));
                } catch(RuntimeException exception) { partial=true; }
            }
            DisclosureEvidenceImportStatus status=partial?DisclosureEvidenceImportStatus.PARTIAL:DisclosureEvidenceImportStatus.SUCCESS;
            return save(stockCode,from,to,status,imported,partial?"one or more disclosure metadata items failed":null,requested);
        } catch(RuntimeException exception) {
            return save(stockCode,from,to,DisclosureEvidenceImportStatus.FAILED,0,sanitize(exception),requested);
        }
    }

    @Override public List<DisclosureEvidenceImportHistory> importWatchlist(LocalDate baseDate) {
        return importTargets(stocks.findAll().stream().filter(Stock::active).map(Stock::stockCode).toList(),baseDate);
    }
    @Override public List<DisclosureEvidenceImportHistory> importHoldings(LocalDate baseDate) {
        return importTargets(positions.findOpenPositions().stream().map(LivePosition::stockCode).toList(),baseDate);
    }
    private List<DisclosureEvidenceImportHistory> importTargets(List<String> codes,LocalDate baseDate) {
        LocalDate from=baseDate.minusDays(properties.getLookbackDays());
        return codes.stream().distinct().map(code->importStock(code,from,baseDate)).toList();
    }
    @Override public List<DisclosureEvidenceImportHistory> findHistories(String stockCode) {
        return histories.findDisclosureImportsByStockCode(stockCode,100);
    }
    @Override public List<CatalystEvidence> findEvidences(String stockCode,LocalDate from,LocalDate to) {
        Instant start=from.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
        Instant end=to.plusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
        return evidences.findEvidenceByStockCode(stockCode).stream()
                .filter(v->v.evidenceType()==CatalystEvidenceType.DART_DISCLOSURE||v.evidenceType()==CatalystEvidenceType.KRX_DISCLOSURE)
                .filter(v->v.sourcePublishedAt()!=null&&!v.sourcePublishedAt().isBefore(start)&&v.sourcePublishedAt().isBefore(end)).toList();
    }
    private Optional<InvestmentCatalyst> relatedCatalyst(DisclosureActualRecord record) {
        if(record.relatedCatalystType()!=CatalystType.EARNINGS
                &&record.relatedCatalystType()!=CatalystType.ORDER_CONTRACT)return Optional.empty();
        return catalysts.find(record.stockCode(),record.disclosureDate().minusDays(30),record.disclosureDate().plusDays(30),null)
                .stream().filter(v->v.catalystType()==record.relatedCatalystType()).findFirst();
    }
    private boolean isDuplicate(DisclosureActualRecord record) {
        if(record.receiptNo()!=null&&!record.receiptNo().isBlank())
            return evidences.findByStockCodeAndReceiptNo(record.stockCode(),record.receiptNo()).isPresent();
        return evidences.findEvidenceByStockCode(record.stockCode()).stream()
                .anyMatch(value->Objects.equals(value.title(),record.title())
                        &&Objects.equals(value.sourceName(),record.source().name())
                        &&value.sourcePublishedAt()!=null
                        &&value.sourcePublishedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate().equals(record.disclosureDate()));
    }
    private DisclosureEvidenceImportHistory save(String stockCode,LocalDate from,LocalDate to,
            DisclosureEvidenceImportStatus status,int count,String reason,Instant requested) {
        DisclosureEvidenceImportHistory value=histories.save(new DisclosureEvidenceImportHistory(null,
                properties.getType(),stockCode,from,to,status,count,reason,requested,clock.instant()));
        metrics.recordDisclosureActualImport(properties.getType().name(),status==DisclosureEvidenceImportStatus.FAILED?"failure":status.name().toLowerCase(Locale.ROOT));
        return value;
    }
    private static String sanitize(RuntimeException e){String value=e.getClass().getSimpleName();return value.length()>1000?value.substring(0,1000):value;}
}
