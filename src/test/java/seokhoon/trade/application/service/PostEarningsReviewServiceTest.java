package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.AnalyzeEarningsUseCase;
import seokhoon.trade.application.port.in.ResearchUseCases.CreatePostEarningsReviewCommand;
import seokhoon.trade.application.port.out.EarningsEventPort;
import seokhoon.trade.application.port.out.EarningsPreviewPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.PostEarningsReviewPort;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PostEarningsReviewServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Test
    void calculatesSurpriseAndDoesNotAutoMutateThesisWhenBroken() {
        InMemoryReviewPort reviews = new InMemoryReviewPort();
        InMemoryEventPort events = new InMemoryEventPort();
        InMemoryPreviewPort previews = new InMemoryPreviewPort();
        CountingAnalyzeUseCase analyzer = new CountingAnalyzeUseCase();
        PostEarningsReviewService service = new PostEarningsReviewService(
                reviews, events, previews, analyzer, OperationalMetricsPort.noop(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        PostEarningsReview saved = service.create(new CreatePostEarningsReviewCommand(
                1L, "005930", LocalDate.of(2026, 7, 31),
                new BigDecimal("1100"), new BigDecimal("180"), new BigDecimal("120"),
                null, ThesisImpact.BROKEN, "Thesis broken by margin miss",
                List.of(), true, false));

        assertThat(saved.revenueSurpriseRate()).isEqualByComparingTo("0.1000");
        assertThat(saved.operatingIncomeSurpriseRate()).isEqualByComparingTo("0.2000");
        assertThat(saved.actualOperatingMargin()).isEqualByComparingTo("0.1636");
        assertThat(saved.actionItems()).anyMatch(item -> item.contains("ACTIVE thesis 수동 점검 필요"));
        assertThat(saved.actionItems()).anyMatch(item -> item.contains("QUARTERLY_FINANCIAL_UPSERT_REQUIRED"));
        assertThat(analyzer.count).isZero();
        assertThat(events.savedStatus).isEqualTo(EarningsEventStatus.REVIEWED);
    }

    private static class InMemoryReviewPort implements PostEarningsReviewPort {
        private final List<PostEarningsReview> values = new ArrayList<>();

        @Override public PostEarningsReview save(PostEarningsReview value) { values.add(value); return value; }
        @Override public Optional<PostEarningsReview> findByEarningsEventId(long earningsEventId) { return Optional.empty(); }
        @Override public List<PostEarningsReview> findReviewsByStockCode(String stockCode) { return values; }
        @Override public List<PostEarningsReview> findByReviewDateBetween(LocalDate from, LocalDate to) { return values; }
        @Override public List<PostEarningsReview> findByThesisImpactIn(List<ThesisImpact> thesisImpacts) { return values; }
    }

    private static class InMemoryEventPort implements EarningsEventPort {
        private EarningsEventStatus savedStatus;

        @Override
        public EarningsEvent save(EarningsEvent value) {
            savedStatus = value.status();
            return value;
        }

        @Override
        public Optional<EarningsEvent> findById(long id) {
            return Optional.of(new EarningsEvent(1L, "005930", 2026, 2,
                    LocalDate.of(2026, 7, 31), null, EarningsEventStatus.ANNOUNCED,
                    null, NOW, NOW));
        }

        @Override public Optional<EarningsEvent> findEventByStockCodeAndQuarter(String stockCode, int fiscalYear, int fiscalQuarter) { return Optional.empty(); }
        @Override public List<EarningsEvent> find(String stockCode, LocalDate from, LocalDate to) { return List.of(); }
        @Override public List<EarningsEvent> findByStatusAndExpectedAnnouncementDateBetween(EarningsEventStatus status, LocalDate from, LocalDate to) { return List.of(); }
    }

    private static class InMemoryPreviewPort implements EarningsPreviewPort {
        @Override public EarningsPreview save(EarningsPreview value) { return value; }
        @Override public Optional<EarningsPreview> findPreviewById(long id) { return Optional.empty(); }
        @Override
        public Optional<EarningsPreview> findLatestByEarningsEventId(long earningsEventId) {
            return Optional.of(new EarningsPreview(1L, 1L, "005930",
                    LocalDate.of(2026, 7, 25), List.of("margin"),
                    new BigDecimal("1000"), new BigDecimal("150"), new BigDecimal("100"),
                    null, List.of(), List.of(), EarningsPreviewStatus.READY, NOW, NOW));
        }
        @Override public List<EarningsPreview> findPreviewsByStockCode(String stockCode) { return List.of(); }
        @Override public List<EarningsPreview> findByStatusAndPreviewDateBetween(EarningsPreviewStatus status, LocalDate from, LocalDate to) { return List.of(); }
    }

    private static class CountingAnalyzeUseCase implements AnalyzeEarningsUseCase {
        private int count;
        @Override public EarningsAnalysisSnapshot analyzeStock(String stockCode, LocalDate baseDate) { count++; return null; }
        @Override public List<EarningsAnalysisSnapshot> analyzeStocks(List<String> stockCodes, LocalDate baseDate) { return List.of(); }
    }
}
