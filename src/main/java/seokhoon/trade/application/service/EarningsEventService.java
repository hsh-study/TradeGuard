package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.ResearchUseCases.CreateEarningsEventCommand;
import seokhoon.trade.application.port.in.ResearchUseCases.EarningsEventUseCase;
import seokhoon.trade.application.port.in.ResearchUseCases.UpdateEarningsEventCommand;
import seokhoon.trade.application.port.out.EarningsEventPort;
import seokhoon.trade.application.port.out.InvestmentCatalystPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.config.ResearchProperties;
import seokhoon.trade.domain.research.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class EarningsEventService implements EarningsEventUseCase {
    private final EarningsEventPort eventPort;
    private final InvestmentCatalystPort catalystPort;
    private final CatalystEvidenceService evidenceService;
    private final ResearchProperties properties;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public EarningsEventService(
            EarningsEventPort eventPort,
            InvestmentCatalystPort catalystPort,
            CatalystEvidenceService evidenceService,
            ResearchProperties properties,
            OperationalMetricsPort metrics
    ) {
        this(eventPort, catalystPort, evidenceService, properties, metrics, Clock.systemUTC());
    }

    EarningsEventService(
            EarningsEventPort eventPort,
            InvestmentCatalystPort catalystPort,
            CatalystEvidenceService evidenceService,
            ResearchProperties properties,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.eventPort = eventPort;
        this.catalystPort = catalystPort;
        this.evidenceService = evidenceService;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    EarningsEventService(
            EarningsEventPort eventPort,
            InvestmentCatalystPort catalystPort,
            ResearchProperties properties,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this(eventPort, catalystPort,
                new CatalystEvidenceService(new NoopCatalystEvidencePort(), OperationalMetricsPort.noop(), clock),
                properties, metrics, clock);
    }

    @Override
    @Transactional
    public EarningsEvent create(CreateEarningsEventCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = clock.instant();
        EarningsEvent saved = eventPort.save(new EarningsEvent(
                null,
                command.stockCode(),
                command.fiscalYear(),
                command.fiscalQuarter(),
                command.expectedAnnouncementDate(),
                command.actualAnnouncementDate(),
                command.status() == null ? EarningsEventStatus.SCHEDULED : command.status(),
                command.memo(),
                now,
                now
        ));
        boolean autoCreate = command.autoCreateCatalyst() == null
                ? properties.isEarningsEventAutoCreateCatalyst()
                : command.autoCreateCatalyst();
        if (autoCreate) {
            createCatalystIfAbsent(saved, now);
        }
        evidenceService.saveSystemEvidence(null, saved.stockCode(), CatalystEvidenceType.EARNINGS_EVENT,
                eventTitle(saved), "Earnings event " + saved.fiscalYear() + "Q" + saved.fiscalQuarter()
                        + " expected=" + saved.expectedAnnouncementDate(),
                "TradeGuard", null, saved.expectedAnnouncementDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                EvidenceConfidence.HIGH);
        metrics.recordResearchEarningsEvent(saved.status().name());
        return saved;
    }

    @Override
    public List<EarningsEvent> find(String stockCode, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        return eventPort.find(stockCode, from, to);
    }

    @Override
    @Transactional
    public EarningsEvent update(long id, UpdateEarningsEventCommand command) {
        EarningsEvent current = required(id);
        EarningsEvent saved = eventPort.save(new EarningsEvent(
                current.id(),
                current.stockCode(),
                current.fiscalYear(),
                current.fiscalQuarter(),
                command.expectedAnnouncementDate() == null
                        ? current.expectedAnnouncementDate()
                        : command.expectedAnnouncementDate(),
                command.actualAnnouncementDate() == null
                        ? current.actualAnnouncementDate()
                        : command.actualAnnouncementDate(),
                command.status() == null ? current.status() : command.status(),
                command.memo() == null ? current.memo() : command.memo(),
                current.createdAt(),
                clock.instant()
        ));
        metrics.recordResearchEarningsEvent(saved.status().name());
        return saved;
    }

    private void createCatalystIfAbsent(EarningsEvent event, Instant now) {
        boolean exists = catalystPort.find(
                        event.stockCode(),
                        event.expectedAnnouncementDate(),
                        event.expectedAnnouncementDate(),
                        null
                )
                .stream()
                .anyMatch(catalyst -> catalyst.catalystType() == CatalystType.EARNINGS
                        && eventTitle(event).equals(catalyst.title()));
        if (!exists) {
            catalystPort.save(new InvestmentCatalyst(
                    null,
                    event.stockCode(),
                    eventTitle(event),
                    CatalystType.EARNINGS,
                    event.expectedAnnouncementDate(),
                    CatalystImportance.HIGH,
                    CatalystStatus.UPCOMING,
                    null,
                    "Auto-created from earnings event " + event.fiscalYear() + "Q" + event.fiscalQuarter(),
                    now,
                    now
            ));
        }
    }

    private EarningsEvent required(long id) {
        return eventPort.findById(id)
                .orElseThrow(() -> new ResearchNotFoundException("Earnings event not found: " + id));
    }

    private static String eventTitle(EarningsEvent event) {
        return "Earnings " + event.fiscalYear() + "Q" + event.fiscalQuarter();
    }
}
