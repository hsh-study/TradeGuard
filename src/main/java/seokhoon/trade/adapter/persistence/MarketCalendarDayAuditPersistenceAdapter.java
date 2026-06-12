package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.MarketCalendarDayAuditPort;
import seokhoon.trade.domain.market.MarketCalendarDayAudit;

import java.time.LocalDate;
import java.util.List;

@Component
public class MarketCalendarDayAuditPersistenceAdapter
        implements MarketCalendarDayAuditPort {
    private final MarketCalendarDayAuditJpaRepository repository;

    public MarketCalendarDayAuditPersistenceAdapter(
            MarketCalendarDayAuditJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public MarketCalendarDayAudit save(MarketCalendarDayAudit audit) {
        return repository.saveAndFlush(MarketCalendarDayAuditEntity.from(audit))
                .toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketCalendarDayAudit> findBetween(
            LocalDate from,
            LocalDate to
    ) {
        return repository.findByTradeDateBetweenOrderByCreatedAtDescIdDesc(from, to)
                .stream()
                .map(MarketCalendarDayAuditEntity::toDomain)
                .toList();
    }
}
