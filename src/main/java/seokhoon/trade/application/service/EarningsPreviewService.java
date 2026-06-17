package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.GenerateEarningsPreviewUseCase;
import seokhoon.trade.application.port.in.ResearchUseCases.CreateEarningsPreviewCommand;
import seokhoon.trade.application.port.in.ResearchUseCases.EarningsPreviewUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.research.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class EarningsPreviewService implements EarningsPreviewUseCase, GenerateEarningsPreviewUseCase {
    private final EarningsPreviewPort previewPort;
    private final EarningsEventPort eventPort;
    private final InvestmentThesisPort thesisPort;
    private final EarningsAnalysisPort analysisPort;
    private final ValuationSnapshotPort valuationPort;
    private final IndicatorSnapshotPort indicatorPort;
    private final InvestmentCatalystPort catalystPort;
    private final CatalystEvidenceService evidenceService;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public EarningsPreviewService(
            EarningsPreviewPort previewPort,
            EarningsEventPort eventPort,
            InvestmentThesisPort thesisPort,
            EarningsAnalysisPort analysisPort,
            ValuationSnapshotPort valuationPort,
            IndicatorSnapshotPort indicatorPort,
            InvestmentCatalystPort catalystPort,
            CatalystEvidenceService evidenceService,
            OperationalMetricsPort metrics
    ) {
        this(previewPort, eventPort, thesisPort, analysisPort, valuationPort,
                indicatorPort, catalystPort, evidenceService, metrics, Clock.systemUTC());
    }

    EarningsPreviewService(
            EarningsPreviewPort previewPort,
            EarningsEventPort eventPort,
            InvestmentThesisPort thesisPort,
            EarningsAnalysisPort analysisPort,
            ValuationSnapshotPort valuationPort,
            IndicatorSnapshotPort indicatorPort,
            InvestmentCatalystPort catalystPort,
            CatalystEvidenceService evidenceService,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.previewPort = previewPort;
        this.eventPort = eventPort;
        this.thesisPort = thesisPort;
        this.analysisPort = analysisPort;
        this.valuationPort = valuationPort;
        this.indicatorPort = indicatorPort;
        this.catalystPort = catalystPort;
        this.evidenceService = evidenceService;
        this.metrics = metrics;
        this.clock = clock;
    }

    EarningsPreviewService(
            EarningsPreviewPort previewPort,
            EarningsEventPort eventPort,
            InvestmentThesisPort thesisPort,
            EarningsAnalysisPort analysisPort,
            ValuationSnapshotPort valuationPort,
            IndicatorSnapshotPort indicatorPort,
            InvestmentCatalystPort catalystPort,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this(previewPort, eventPort, thesisPort, analysisPort, valuationPort,
                indicatorPort, catalystPort,
                new CatalystEvidenceService(new NoopCatalystEvidencePort(), OperationalMetricsPort.noop(), clock),
                metrics, clock);
    }

    @Override
    public EarningsPreview create(CreateEarningsPreviewCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = clock.instant();
        EarningsPreview saved = previewPort.save(new EarningsPreview(
                null,
                command.earningsEventId(),
                command.stockCode(),
                command.previewDate(),
                list(command.keyCheckpoints()),
                command.expectedRevenue(),
                command.expectedOperatingIncome(),
                command.expectedNetIncome(),
                command.expectedOperatingMargin(),
                list(command.expectedRisks()),
                list(command.thesisWatchPoints()),
                command.status() == null ? EarningsPreviewStatus.DRAFT : command.status(),
                now,
                now
        ));
        if (saved.status() == EarningsPreviewStatus.READY) {
            evidenceService.saveSystemEvidence(null, saved.stockCode(), CatalystEvidenceType.EARNINGS_PREVIEW,
                    "Earnings preview " + saved.earningsEventId(),
                    "READY earnings preview checkpoints=" + saved.keyCheckpoints(),
                    "TradeGuard", null, saved.previewDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                    EvidenceConfidence.MEDIUM);
        }
        metrics.recordResearchEarningsPreview(saved.status() == EarningsPreviewStatus.READY ? "ready" : "created");
        return saved;
    }

    @Override
    public List<EarningsPreview> findByStockCode(String stockCode) {
        return previewPort.findPreviewsByStockCode(stockCode);
    }

    @Override
    public List<EarningsPreview> findUpcomingReady(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        return previewPort.findByStatusAndPreviewDateBetween(EarningsPreviewStatus.READY, from, to);
    }

    @Override
    public EarningsPreview generate(String stockCode, long earningsEventId, LocalDate previewDate) {
        EarningsEvent event = eventPort.findById(earningsEventId)
                .orElseThrow(() -> new ResearchNotFoundException("Earnings event not found: " + earningsEventId));
        if (!event.stockCode().equals(stockCode)) {
            throw new IllegalArgumentException("stockCode does not match earnings event");
        }
        List<String> checkpoints = new ArrayList<>();
        List<String> risks = new ArrayList<>();
        List<String> watchPoints = new ArrayList<>();

        thesisPort.find(stockCode, ThesisStatus.ACTIVE)
                .forEach(thesis -> watchPoints.add("THESIS " + thesis.title() + ": " + thesis.coreAssumption()
                        + " / invalidation=" + thesis.invalidationCondition()));
        analysisPort.findLatestByStockCode(stockCode).ifPresentOrElse(analysis -> {
            checkpoints.add("LATEST_EARNINGS_STATUS " + analysis.status() + " overallScore=" + analysis.overallScore());
            if (analysis.status() == EarningsAnalysisStatus.WEAK) {
                risks.add("Latest earnings analysis is WEAK");
            }
        }, () -> risks.add("Latest earnings analysis unavailable"));
        valuationPort.findLatestByStockCode(stockCode, previewDate).ifPresent(valuation ->
                checkpoints.add("VALUATION per=" + valuation.per() + " pbr=" + valuation.pbr() + " psr=" + valuation.psr()));
        latestIndicator(stockCode, previewDate).ifPresentOrElse(indicator ->
                checkpoints.add("TECHNICAL ma20=" + indicator.ma20() + " rsi14=" + indicator.rsi14()),
                () -> risks.add("Technical indicator snapshot unavailable"));
        catalystPort.find(stockCode, previewDate, event.expectedAnnouncementDate(), CatalystStatus.UPCOMING)
                .forEach(catalyst -> checkpoints.add("UPCOMING_CATALYST " + catalyst.expectedDate() + " " + catalyst.title()));
        if (checkpoints.isEmpty()) {
            checkpoints.add("Check revenue, operating income, margin, cash flow, and guidance manually");
        }

        return create(new CreateEarningsPreviewCommand(
                earningsEventId,
                stockCode,
                previewDate,
                checkpoints,
                null,
                null,
                null,
                null,
                risks,
                watchPoints,
                EarningsPreviewStatus.DRAFT
        ));
    }

    private java.util.Optional<IndicatorSnapshot> latestIndicator(String stockCode, LocalDate baseDate) {
        return indicatorPort.findByStockCodeAndTradeDateBetween(stockCode, baseDate.minusDays(180), baseDate)
                .stream()
                .max(Comparator.comparing(IndicatorSnapshot::tradeDate));
    }

    private static List<String> list(List<String> values) {
        return values == null ? List.of() : values;
    }
}
