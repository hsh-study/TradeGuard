package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ResearchUseCases.CreateThesisCommand;
import seokhoon.trade.application.port.in.ResearchUseCases.UpdateThesisCommand;
import seokhoon.trade.application.port.out.InvestmentThesisPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.research.InvestmentThesis;
import seokhoon.trade.domain.research.ThesisStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class InvestmentThesisServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsFindsUpdatesAndClosesThesis() {
        InMemoryThesisPort port = new InMemoryThesisPort();
        InvestmentThesisService service =
                new InvestmentThesisService(port, OperationalMetricsPort.noop(), CLOCK);

        InvestmentThesis created = service.create(new CreateThesisCommand(
                "005930", "HBM recovery", "memory cycle improves",
                "quarterly margin declines", new BigDecimal("90000"),
                "close below MA60", 75, ThesisStatus.WATCH));
        InvestmentThesis updated = service.update(created.id(), new UpdateThesisCommand(
                "HBM recovery confirmed", null, null, new BigDecimal("95000"),
                null, 85, ThesisStatus.ACTIVE));
        InvestmentThesis closed = service.close(created.id());

        assertThat(service.find("005930")).hasSize(1);
        assertThat(updated.title()).isEqualTo("HBM recovery confirmed");
        assertThat(updated.targetPrice()).isEqualByComparingTo("95000");
        assertThat(closed.status()).isEqualTo(ThesisStatus.CLOSED);
    }

    private static class InMemoryThesisPort implements InvestmentThesisPort {
        private final Map<Long, InvestmentThesis> values = new HashMap<>();
        private long sequence;

        @Override
        public InvestmentThesis save(InvestmentThesis thesis) {
            long id = thesis.id() == null ? ++sequence : thesis.id();
            InvestmentThesis saved = new InvestmentThesis(
                    id, thesis.stockCode(), thesis.title(), thesis.coreAssumption(),
                    thesis.invalidationCondition(), thesis.targetPrice(), thesis.stopLossCondition(),
                    thesis.confidence(), thesis.status(), thesis.createdAt(), thesis.updatedAt());
            values.put(id, saved);
            return saved;
        }

        @Override
        public Optional<InvestmentThesis> findThesisById(long id) {
            return Optional.ofNullable(values.get(id));
        }

        @Override
        public List<InvestmentThesis> find(String stockCode, ThesisStatus status) {
            return values.values().stream()
                    .filter(value -> stockCode == null || value.stockCode().equals(stockCode))
                    .filter(value -> status == null || value.status() == status)
                    .toList();
        }
    }
}
