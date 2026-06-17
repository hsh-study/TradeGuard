package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ResearchUseCases.CreateEarningsEventCommand;
import seokhoon.trade.application.port.out.EarningsEventPort;
import seokhoon.trade.application.port.out.InvestmentCatalystPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.config.ResearchProperties;
import seokhoon.trade.domain.research.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EarningsEventServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Test
    void createsEventAndEarningsCatalystWhenAbsent() {
        InMemoryEventPort events = new InMemoryEventPort();
        InMemoryCatalystPort catalysts = new InMemoryCatalystPort();
        CatalystEvidenceServiceTest.InMemoryEvidencePort evidences =
                new CatalystEvidenceServiceTest.InMemoryEvidencePort();
        EarningsEventService service = service(events, catalysts, evidences);

        EarningsEvent saved = service.create(command());

        assertThat(saved.status()).isEqualTo(EarningsEventStatus.SCHEDULED);
        assertThat(catalysts.values).hasSize(1);
        assertThat(catalysts.values.get(0).catalystType()).isEqualTo(CatalystType.EARNINGS);
        assertThat(evidences.findEvidenceByStockCode("005930"))
                .anyMatch(evidence -> evidence.evidenceType() == CatalystEvidenceType.EARNINGS_EVENT);
    }

    @Test
    void doesNotCreateDuplicateEarningsCatalyst() {
        InMemoryEventPort events = new InMemoryEventPort();
        InMemoryCatalystPort catalysts = new InMemoryCatalystPort();
        catalysts.values.add(new InvestmentCatalyst(1L, "005930", "Earnings 2026Q2",
                CatalystType.EARNINGS, LocalDate.of(2026, 7, 31),
                CatalystImportance.HIGH, CatalystStatus.UPCOMING, null, null, NOW, NOW));
        EarningsEventService service = service(events, catalysts);

        service.create(command());

        assertThat(catalysts.values).hasSize(1);
    }

    private static EarningsEventService service(InMemoryEventPort events, InMemoryCatalystPort catalysts) {
        return service(events, catalysts, new CatalystEvidenceServiceTest.InMemoryEvidencePort());
    }

    private static EarningsEventService service(
            InMemoryEventPort events,
            InMemoryCatalystPort catalysts,
            CatalystEvidenceServiceTest.InMemoryEvidencePort evidences
    ) {
        ResearchProperties properties = new ResearchProperties();
        properties.setEarningsEventAutoCreateCatalyst(true);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        return new EarningsEventService(events, catalysts,
                new CatalystEvidenceService(evidences, OperationalMetricsPort.noop(), clock),
                properties, OperationalMetricsPort.noop(), clock);
    }

    private static CreateEarningsEventCommand command() {
        return new CreateEarningsEventCommand("005930", 2026, 2,
                LocalDate.of(2026, 7, 31), null, null, null, null);
    }

    private static class InMemoryEventPort implements EarningsEventPort {
        private final List<EarningsEvent> values = new ArrayList<>();

        @Override
        public EarningsEvent save(EarningsEvent value) {
            EarningsEvent saved = new EarningsEvent(1L, value.stockCode(), value.fiscalYear(),
                    value.fiscalQuarter(), value.expectedAnnouncementDate(),
                    value.actualAnnouncementDate(), value.status(), value.memo(),
                    value.createdAt(), value.updatedAt());
            values.add(saved);
            return saved;
        }

        @Override public Optional<EarningsEvent> findById(long id) { return values.stream().findFirst(); }
        @Override public Optional<EarningsEvent> findEventByStockCodeAndQuarter(String stockCode, int fiscalYear, int fiscalQuarter) { return Optional.empty(); }
        @Override public List<EarningsEvent> find(String stockCode, LocalDate from, LocalDate to) { return values; }
        @Override public List<EarningsEvent> findByStatusAndExpectedAnnouncementDateBetween(EarningsEventStatus status, LocalDate from, LocalDate to) { return values; }
    }

    private static class InMemoryCatalystPort implements InvestmentCatalystPort {
        private final List<InvestmentCatalyst> values = new ArrayList<>();

        @Override
        public InvestmentCatalyst save(InvestmentCatalyst catalyst) {
            values.add(catalyst);
            return catalyst;
        }

        @Override public Optional<InvestmentCatalyst> findCatalystById(long id) { return Optional.empty(); }

        @Override
        public List<InvestmentCatalyst> find(String stockCode, LocalDate from, LocalDate to, CatalystStatus status) {
            return values.stream()
                    .filter(value -> stockCode == null || stockCode.equals(value.stockCode()))
                    .filter(value -> from == null || !value.expectedDate().isBefore(from))
                    .filter(value -> to == null || !value.expectedDate().isAfter(to))
                    .toList();
        }
    }
}
