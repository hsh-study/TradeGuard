package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.in.ResearchUseCases.*;
import seokhoon.trade.application.port.out.InvestmentCatalystPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.research.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class InvestmentCatalystService implements CatalystUseCase {
    private final InvestmentCatalystPort port;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public InvestmentCatalystService(InvestmentCatalystPort port, OperationalMetricsPort metrics) {
        this(port, metrics, Clock.systemUTC());
    }

    InvestmentCatalystService(InvestmentCatalystPort port, OperationalMetricsPort metrics, Clock clock) {
        this.port = port;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    public InvestmentCatalyst create(CreateCatalystCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = clock.instant();
        CatalystStatus status = command.status() == null ? CatalystStatus.UPCOMING : command.status();
        InvestmentCatalyst saved = port.save(new InvestmentCatalyst(
                null, command.stockCode(), command.title(), command.catalystType(),
                command.expectedDate(), command.importance(), status, command.sourceUrl(),
                command.memo(), now, now
        ));
        metrics.recordResearchCatalyst(saved.status().name());
        return saved;
    }

    @Override
    public List<InvestmentCatalyst> find(String stockCode, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        return port.find(stockCode, from, to, null);
    }

    @Override
    public InvestmentCatalyst update(long id, UpdateCatalystCommand command) {
        InvestmentCatalyst current = port.findCatalystById(id)
                .orElseThrow(() -> new ResearchNotFoundException("Investment catalyst not found: " + id));
        InvestmentCatalyst saved = port.save(new InvestmentCatalyst(
                current.id(),
                command.stockCode() == null ? current.stockCode() : command.stockCode(),
                value(command.title(), current.title()),
                command.catalystType() == null ? current.catalystType() : command.catalystType(),
                command.expectedDate() == null ? current.expectedDate() : command.expectedDate(),
                command.importance() == null ? current.importance() : command.importance(),
                command.status() == null ? current.status() : command.status(),
                command.sourceUrl() == null ? current.sourceUrl() : command.sourceUrl(),
                command.memo() == null ? current.memo() : command.memo(),
                current.createdAt(),
                clock.instant()
        ));
        metrics.recordResearchCatalyst(saved.status().name());
        return saved;
    }

    private static String value(String candidate, String fallback) {
        return candidate == null ? fallback : candidate;
    }
}
