package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.AnalyzeEarningsUseCase;
import seokhoon.trade.application.port.in.ResearchUseCases.CreatePostEarningsReviewCommand;
import seokhoon.trade.application.port.in.ResearchUseCases.PostEarningsReviewUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PostEarningsReviewService implements PostEarningsReviewUseCase {
    private static final int SCALE = 4;

    private final PostEarningsReviewPort reviewPort;
    private final EarningsEventPort eventPort;
    private final EarningsPreviewPort previewPort;
    private final AnalyzeEarningsUseCase analyzeEarningsUseCase;
    private final CatalystEvidenceService evidenceService;
    private final OperationalMetricsPort metrics;
    private final EarningsConsensusPort consensusPort;
    private final Clock clock;

    @Autowired
    public PostEarningsReviewService(
            PostEarningsReviewPort reviewPort,
            EarningsEventPort eventPort,
            EarningsPreviewPort previewPort,
            AnalyzeEarningsUseCase analyzeEarningsUseCase,
            CatalystEvidenceService evidenceService,
            EarningsConsensusPort consensusPort,
            OperationalMetricsPort metrics
    ) {
        this(reviewPort, eventPort, previewPort, analyzeEarningsUseCase,
                evidenceService, consensusPort, metrics, Clock.systemUTC());
    }

    PostEarningsReviewService(
            PostEarningsReviewPort reviewPort,
            EarningsEventPort eventPort,
            EarningsPreviewPort previewPort,
            AnalyzeEarningsUseCase analyzeEarningsUseCase,
            CatalystEvidenceService evidenceService,
            EarningsConsensusPort consensusPort,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.reviewPort = reviewPort;
        this.eventPort = eventPort;
        this.previewPort = previewPort;
        this.analyzeEarningsUseCase = analyzeEarningsUseCase;
        this.evidenceService = evidenceService;
        this.consensusPort = consensusPort;
        this.metrics = metrics;
        this.clock = clock;
    }

    PostEarningsReviewService(
            PostEarningsReviewPort reviewPort,
            EarningsEventPort eventPort,
            EarningsPreviewPort previewPort,
            AnalyzeEarningsUseCase analyzeEarningsUseCase,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this(reviewPort, eventPort, previewPort, analyzeEarningsUseCase,
                new CatalystEvidenceService(new NoopCatalystEvidencePort(), OperationalMetricsPort.noop(), clock),
                EarningsConsensusPort.noop(),
                metrics, clock);
    }

    @Override
    @Transactional
    public PostEarningsReview create(CreatePostEarningsReviewCommand command) {
        Objects.requireNonNull(command, "command");
        EarningsEvent event = eventPort.findById(command.earningsEventId())
                .orElseThrow(() -> new ResearchNotFoundException("Earnings event not found: " + command.earningsEventId()));
        BigDecimal actualOperatingMargin = command.actualOperatingMargin() == null
                ? ratio(command.actualOperatingIncome(), command.actualRevenue())
                : command.actualOperatingMargin();
        EarningsPreview preview = previewPort.findLatestByEarningsEventId(command.earningsEventId()).orElse(null);
        EarningsConsensusSnapshot consensus=consensusPort.findLatest(command.stockCode(),event.fiscalYear(),event.fiscalQuarter()).orElse(null);
        BigDecimal expectedRevenue=preview==null?null:preview.expectedRevenue();BigDecimal expectedOperating=preview==null?null:preview.expectedOperatingIncome();BigDecimal expectedNet=preview==null?null:preview.expectedNetIncome();
        boolean consensusUsed=false;
        if(expectedRevenue==null&&consensus!=null){expectedRevenue=consensus.expectedRevenue();consensusUsed=true;}
        if(expectedOperating==null&&consensus!=null){expectedOperating=consensus.expectedOperatingIncome();consensusUsed=true;}
        if(expectedNet==null&&consensus!=null){expectedNet=consensus.expectedNetIncome();consensusUsed=true;}
        BigDecimal revenueSurprise=surprise(command.actualRevenue(),expectedRevenue);
        BigDecimal operatingIncomeSurprise=surprise(command.actualOperatingIncome(),expectedOperating);
        BigDecimal netIncomeSurprise=surprise(command.actualNetIncome(),expectedNet);

        List<String> actionItems = new ArrayList<>(command.actionItems() == null ? List.of() : command.actionItems());
        if (command.thesisImpact() == ThesisImpact.BROKEN) {
            actionItems.add("EARNINGS_THESIS_BROKEN_REVIEW_REQUIRED: ACTIVE thesis 수동 점검 필요");
        }
        if (command.upsertQuarterlyFinancial()) {
            actionItems.add("QUARTERLY_FINANCIAL_UPSERT_REQUIRED: total assets/liabilities/equity/cash flow 입력 후 upsert");
        }
        if(consensusUsed)actionItems.add("CONSENSUS_SURPRISE_USED");
        Instant now = clock.instant();
        PostEarningsReview saved = reviewPort.save(new PostEarningsReview(
                null,
                command.earningsEventId(),
                command.stockCode(),
                command.reviewDate(),
                command.actualRevenue(),
                command.actualOperatingIncome(),
                command.actualNetIncome(),
                actualOperatingMargin,
                revenueSurprise,
                operatingIncomeSurprise,
                netIncomeSurprise,
                consensusUsed,
                command.thesisImpact(),
                command.reviewSummary(),
                actionItems,
                now,
                now
        ));
        eventPort.save(new EarningsEvent(
                event.id(), event.stockCode(), event.fiscalYear(), event.fiscalQuarter(),
                event.expectedAnnouncementDate(),
                event.actualAnnouncementDate() == null ? command.reviewDate() : event.actualAnnouncementDate(),
                EarningsEventStatus.REVIEWED,
                event.memo(),
                event.createdAt(),
                now
        ));
        if (command.rerunEarningsAnalysis()) {
            analyzeEarningsUseCase.analyzeStock(command.stockCode(), command.reviewDate());
        }
        evidenceService.saveSystemEvidence(null, saved.stockCode(), CatalystEvidenceType.POST_EARNINGS_REVIEW,
                "Post earnings review " + saved.earningsEventId(),
                saved.reviewSummary(),
                "TradeGuard", null, saved.reviewDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                EvidenceConfidence.HIGH);
        metrics.recordResearchPostEarningsReview(saved.thesisImpact().name());
        return saved;
    }

    @Override
    public List<PostEarningsReview> findByStockCode(String stockCode) {
        return reviewPort.findReviewsByStockCode(stockCode);
    }

    private static BigDecimal surprise(BigDecimal actual, BigDecimal expected) {
        if (expected == null || expected.signum() == 0) {
            return null;
        }
        return actual.subtract(expected).divide(expected.abs(), SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return null;
        }
        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }
}
