package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.OverrideMarketCalendarDayCommand;
import seokhoon.trade.application.port.out.MarketCalendarDayAuditPort;
import seokhoon.trade.application.port.out.MarketCalendarDayPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarDayAudit;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MarketCalendarManagementServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-12T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void createsManualOverrideAndAuditWhenDayDoesNotExist() {
        InMemoryDayPort dayPort = new InMemoryDayPort();
        InMemoryAuditPort auditPort = new InMemoryAuditPort();
        MarketCalendarManagementService service = service(dayPort, auditPort);

        var result = service.override(new OverrideMarketCalendarDayCommand(
                null,
                LocalDate.of(2026, 8, 17),
                false,
                "TEMPORARY_CLOSURE",
                "KRX notice correction",
                null
        ));

        assertThat(result.day().source())
                .isEqualTo(MarketCalendarSource.MANUAL_OVERRIDE);
        assertThat(result.audit().beforeTradingDay()).isNull();
        assertThat(result.audit().afterTradingDay()).isFalse();
        assertThat(result.audit().actor()).isEqualTo("MANUAL_API");
        assertThat(auditPort.audits).hasSize(1);
    }

    @Test
    void updatesExistingDayAndRecordsBeforeAndAfterValues() {
        LocalDate date = LocalDate.of(2026, 8, 17);
        InMemoryDayPort dayPort = new InMemoryDayPort(List.of(new MarketCalendarDay(
                MarketCalendarDay.KRX_STOCK,
                date,
                true,
                null,
                MarketCalendarSource.KRX_OFFICIAL
        )));
        InMemoryAuditPort auditPort = new InMemoryAuditPort();
        MarketCalendarManagementService service = service(dayPort, auditPort);

        var result = service.override(new OverrideMarketCalendarDayCommand(
                MarketCalendarDay.KRX_STOCK,
                date,
                false,
                "TEMPORARY_CLOSURE",
                "Official correction",
                "operator-1"
        ));

        assertThat(result.audit().beforeTradingDay()).isTrue();
        assertThat(result.audit().afterTradingDay()).isFalse();
        assertThat(result.audit().beforeHolidayName()).isNull();
        assertThat(result.audit().afterHolidayName())
                .isEqualTo("TEMPORARY_CLOSURE");
        assertThat(result.audit().actor()).isEqualTo("operator-1");
        assertThat(dayPort.findByDate(date).orElseThrow().source())
                .isEqualTo(MarketCalendarSource.MANUAL_OVERRIDE);
    }

    @Test
    void validationReportsMissingDaysWeekendTradingAndSourceDistribution() {
        InMemoryDayPort dayPort = new InMemoryDayPort(List.of(
                day(LocalDate.of(2026, 1, 1), false, MarketCalendarSource.KRX_OFFICIAL),
                day(LocalDate.of(2026, 1, 2), true, MarketCalendarSource.FALLBACK_GENERATED),
                day(LocalDate.of(2026, 1, 3), true, MarketCalendarSource.MANUAL_OVERRIDE),
                day(LocalDate.of(2026, 6, 15), true, MarketCalendarSource.KRX_OFFICIAL)
        ));
        MarketCalendarManagementService service =
                service(dayPort, new InMemoryAuditPort());

        var result = service.validate(2026);

        assertThat(result.totalDays()).isEqualTo(4);
        assertThat(result.missingDays()).hasSize(361);
        assertThat(result.weekendTradingDays())
                .containsExactly(LocalDate.of(2026, 1, 3));
        assertThat(result.weekdayHolidays())
                .containsExactly(LocalDate.of(2026, 1, 1));
        assertThat(result.sourceDistribution())
                .containsEntry("MANUAL_OVERRIDE", 1)
                .containsEntry("KRX_OFFICIAL", 2)
                .containsEntry("FALLBACK_GENERATED", 1);
        assertThat(result.warnings())
                .anyMatch(warning -> warning.startsWith("MISSING_DAYS"))
                .anyMatch(warning -> warning.startsWith("WEEKEND_TRADING_DAYS"));
    }

    private static MarketCalendarManagementService service(
            InMemoryDayPort dayPort,
            InMemoryAuditPort auditPort
    ) {
        return new MarketCalendarManagementService(
                dayPort,
                auditPort,
                OperationalMetricsPort.noop(),
                CLOCK
        );
    }

    private static MarketCalendarDay day(
            LocalDate date,
            boolean tradingDay,
            MarketCalendarSource source
    ) {
        return new MarketCalendarDay(
                MarketCalendarDay.KRX_STOCK,
                date,
                tradingDay,
                tradingDay ? null : "HOLIDAY",
                source
        );
    }

    private static class InMemoryDayPort implements MarketCalendarDayPort {
        private final List<MarketCalendarDay> days = new ArrayList<>();

        private InMemoryDayPort() {
        }

        private InMemoryDayPort(List<MarketCalendarDay> days) {
            this.days.addAll(days);
        }

        @Override
        public void upsertAll(List<MarketCalendarDay> newDays) {
            newDays.forEach(this::save);
        }

        @Override
        public MarketCalendarDay save(MarketCalendarDay day) {
            days.removeIf(existing -> existing.date().equals(day.date()));
            days.add(day);
            return day;
        }

        @Override
        public Optional<MarketCalendarDay> findByDate(LocalDate date) {
            return days.stream()
                    .filter(day -> day.date().equals(date))
                    .findFirst();
        }

        @Override
        public List<MarketCalendarDay> findBetween(LocalDate from, LocalDate to) {
            return days.stream()
                    .filter(day -> !day.date().isBefore(from))
                    .filter(day -> !day.date().isAfter(to))
                    .sorted(Comparator.comparing(MarketCalendarDay::date))
                    .toList();
        }

        @Override
        public boolean existsByYear(int year) {
            return days.stream().anyMatch(day -> day.date().getYear() == year);
        }

        @Override
        public Optional<MarketCalendarDay> findPreviousTradingDay(LocalDate date) {
            return Optional.empty();
        }

        @Override
        public Optional<MarketCalendarDay> findNextTradingDay(LocalDate date) {
            return Optional.empty();
        }
    }

    private static class InMemoryAuditPort implements MarketCalendarDayAuditPort {
        private final List<MarketCalendarDayAudit> audits = new ArrayList<>();

        @Override
        public MarketCalendarDayAudit save(MarketCalendarDayAudit audit) {
            MarketCalendarDayAudit saved = new MarketCalendarDayAudit(
                    (long) audits.size() + 1,
                    audit.market(),
                    audit.date(),
                    audit.beforeTradingDay(),
                    audit.afterTradingDay(),
                    audit.beforeHolidayName(),
                    audit.afterHolidayName(),
                    audit.reason(),
                    audit.actor(),
                    audit.createdAt()
            );
            audits.add(saved);
            return saved;
        }

        @Override
        public List<MarketCalendarDayAudit> findBetween(
                LocalDate from,
                LocalDate to
        ) {
            return audits.stream()
                    .filter(audit -> !audit.date().isBefore(from))
                    .filter(audit -> !audit.date().isAfter(to))
                    .sorted(Comparator.comparing(
                            MarketCalendarDayAudit::createdAt
                    ).reversed())
                    .toList();
        }
    }
}
