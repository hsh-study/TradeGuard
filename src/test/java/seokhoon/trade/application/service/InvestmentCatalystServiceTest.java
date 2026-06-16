package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ResearchUseCases.CreateCatalystCommand;
import seokhoon.trade.application.port.in.ResearchUseCases.UpdateCatalystCommand;
import seokhoon.trade.application.port.out.InvestmentCatalystPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.research.*;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class InvestmentCatalystServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsFindsAndUpdatesCatalyst() {
        InMemoryCatalystPort port = new InMemoryCatalystPort();
        InvestmentCatalystService service =
                new InvestmentCatalystService(port, OperationalMetricsPort.noop(), CLOCK);
        LocalDate expectedDate = LocalDate.of(2026, 7, 1);

        InvestmentCatalyst created = service.create(new CreateCatalystCommand(
                "005930", "2Q earnings", CatalystType.EARNINGS, expectedDate,
                CatalystImportance.HIGH, null, "https://example.com", "consensus check"));
        InvestmentCatalyst updated = service.update(created.id(), new UpdateCatalystCommand(
                null, null, null, null, null, CatalystStatus.OCCURRED, null, "released"));

        assertThat(service.find("005930", expectedDate, expectedDate)).hasSize(1);
        assertThat(updated.status()).isEqualTo(CatalystStatus.OCCURRED);
        assertThat(updated.memo()).isEqualTo("released");
    }

    private static class InMemoryCatalystPort implements InvestmentCatalystPort {
        private final Map<Long, InvestmentCatalyst> values = new HashMap<>();
        private long sequence;

        @Override
        public InvestmentCatalyst save(InvestmentCatalyst catalyst) {
            long id = catalyst.id() == null ? ++sequence : catalyst.id();
            InvestmentCatalyst saved = new InvestmentCatalyst(
                    id, catalyst.stockCode(), catalyst.title(), catalyst.catalystType(),
                    catalyst.expectedDate(), catalyst.importance(), catalyst.status(),
                    catalyst.sourceUrl(), catalyst.memo(), catalyst.createdAt(), catalyst.updatedAt());
            values.put(id, saved);
            return saved;
        }

        @Override
        public Optional<InvestmentCatalyst> findCatalystById(long id) {
            return Optional.ofNullable(values.get(id));
        }

        @Override
        public List<InvestmentCatalyst> find(
                String stockCode, LocalDate from, LocalDate to, CatalystStatus status
        ) {
            return values.values().stream()
                    .filter(value -> stockCode == null || stockCode.equals(value.stockCode()))
                    .filter(value -> from == null || !value.expectedDate().isBefore(from))
                    .filter(value -> to == null || !value.expectedDate().isAfter(to))
                    .filter(value -> status == null || status == value.status())
                    .toList();
        }
    }
}
