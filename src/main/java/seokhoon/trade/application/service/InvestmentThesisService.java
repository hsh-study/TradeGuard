package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.in.ResearchUseCases.*;
import seokhoon.trade.application.port.out.InvestmentThesisPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.research.InvestmentThesis;
import seokhoon.trade.domain.research.ThesisStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class InvestmentThesisService implements ThesisUseCase {
    private final InvestmentThesisPort port;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public InvestmentThesisService(InvestmentThesisPort port, OperationalMetricsPort metrics) {
        this(port, metrics, Clock.systemUTC());
    }

    InvestmentThesisService(InvestmentThesisPort port, OperationalMetricsPort metrics, Clock clock) {
        this.port = port;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    public InvestmentThesis create(CreateThesisCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = clock.instant();
        ThesisStatus status = command.status() == null ? ThesisStatus.ACTIVE : command.status();
        InvestmentThesis saved = port.save(new InvestmentThesis(
                null, command.stockCode(), command.title(), command.coreAssumption(),
                command.invalidationCondition(), command.targetPrice(), command.stopLossCondition(),
                command.confidence(), status, now, now
        ));
        metrics.recordResearchThesis(saved.status().name());
        return saved;
    }

    @Override
    public List<InvestmentThesis> find(String stockCode) {
        return port.find(stockCode, null);
    }

    @Override
    public InvestmentThesis update(long id, UpdateThesisCommand command) {
        InvestmentThesis current = required(id);
        Instant now = clock.instant();
        InvestmentThesis saved = port.save(new InvestmentThesis(
                current.id(),
                current.stockCode(),
                value(command.title(), current.title()),
                value(command.coreAssumption(), current.coreAssumption()),
                value(command.invalidationCondition(), current.invalidationCondition()),
                command.targetPrice() == null ? current.targetPrice() : command.targetPrice(),
                value(command.stopLossCondition(), current.stopLossCondition()),
                command.confidence() == null ? current.confidence() : command.confidence(),
                command.status() == null ? current.status() : command.status(),
                current.createdAt(),
                now
        ));
        metrics.recordResearchThesis(saved.status().name());
        return saved;
    }

    @Override
    public InvestmentThesis close(long id) {
        InvestmentThesis current = required(id);
        InvestmentThesis saved = port.save(new InvestmentThesis(
                current.id(), current.stockCode(), current.title(), current.coreAssumption(),
                current.invalidationCondition(), current.targetPrice(), current.stopLossCondition(),
                current.confidence(), ThesisStatus.CLOSED, current.createdAt(), clock.instant()
        ));
        metrics.recordResearchThesis(saved.status().name());
        return saved;
    }

    private InvestmentThesis required(long id) {
        return port.findThesisById(id)
                .orElseThrow(() -> new ResearchNotFoundException("Investment thesis not found: " + id));
    }

    private static String value(String candidate, String fallback) {
        return candidate == null ? fallback : candidate;
    }
}
