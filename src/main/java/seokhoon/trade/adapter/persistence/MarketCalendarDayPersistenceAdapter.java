package seokhoon.trade.adapter.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.MarketCalendarDayPort;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class MarketCalendarDayPersistenceAdapter implements MarketCalendarDayPort {
    private final MarketCalendarDayJpaRepository repository;
    private final Clock clock;

    @Autowired
    public MarketCalendarDayPersistenceAdapter(MarketCalendarDayJpaRepository repository) {
        this(repository, Clock.systemUTC());
    }

    MarketCalendarDayPersistenceAdapter(
            MarketCalendarDayJpaRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void upsertAll(List<MarketCalendarDay> days) {
        Instant now = Instant.now(clock);
        List<MarketCalendarDayEntity> entities = days.stream()
                .map(day -> repository.findByMarketAndTradeDate(
                                day.market(),
                                day.date()
                        )
                        .map(entity -> {
                            if (entity.toDomain().source()
                                    != MarketCalendarSource.MANUAL_OVERRIDE
                                    || day.source()
                                    == MarketCalendarSource.MANUAL_OVERRIDE) {
                                entity.update(day, now);
                            }
                            return entity;
                        })
                        .orElseGet(() -> MarketCalendarDayEntity.create(day, now)))
                .toList();
        repository.saveAll(entities);
        repository.flush();
    }

    @Override
    @Transactional
    public MarketCalendarDay save(MarketCalendarDay day) {
        Instant now = Instant.now(clock);
        MarketCalendarDayEntity entity = repository.findByMarketAndTradeDate(
                        day.market(),
                        day.date()
                )
                .map(existing -> {
                    existing.update(day, now);
                    return existing;
                })
                .orElseGet(() -> MarketCalendarDayEntity.create(day, now));
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketCalendarDay> findByDate(LocalDate date) {
        return repository.findByMarketAndTradeDate(MarketCalendarDay.KRX_STOCK, date)
                .map(MarketCalendarDayEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketCalendarDay> findBetween(LocalDate from, LocalDate to) {
        return repository.findByMarketAndTradeDateBetweenOrderByTradeDateAsc(
                        MarketCalendarDay.KRX_STOCK,
                        from,
                        to
                )
                .stream()
                .map(MarketCalendarDayEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByYear(int year) {
        return repository.existsByMarketAndTradeDateBetween(
                MarketCalendarDay.KRX_STOCK,
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketCalendarDay> findPreviousTradingDay(LocalDate date) {
        return repository
                .findFirstByMarketAndTradingDayTrueAndTradeDateBeforeOrderByTradeDateDesc(
                        MarketCalendarDay.KRX_STOCK,
                        date
                )
                .map(MarketCalendarDayEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketCalendarDay> findNextTradingDay(LocalDate date) {
        return repository
                .findFirstByMarketAndTradingDayTrueAndTradeDateAfterOrderByTradeDateAsc(
                        MarketCalendarDay.KRX_STOCK,
                        date
                )
                .map(MarketCalendarDayEntity::toDomain);
    }
}
