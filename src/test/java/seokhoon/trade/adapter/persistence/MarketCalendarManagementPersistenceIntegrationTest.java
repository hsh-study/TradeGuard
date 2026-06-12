package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.MarketCalendarDayAuditPort;
import seokhoon.trade.application.port.out.MarketCalendarDayPort;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarDayAudit;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MarketCalendarManagementPersistenceIntegrationTest {
    @Autowired
    private MarketCalendarDayPort dayPort;

    @Autowired
    private MarketCalendarDayAuditPort auditPort;

    @Test
    void preservesManualOverrideDuringLaterSyncUpsert() {
        LocalDate date = LocalDate.of(2026, 8, 17);
        dayPort.save(day(date, false, MarketCalendarSource.MANUAL_OVERRIDE));

        dayPort.upsertAll(List.of(day(
                date,
                true,
                MarketCalendarSource.KRX_OFFICIAL
        )));

        assertThat(dayPort.findByDate(date))
                .hasValueSatisfying(day -> {
                    assertThat(day.tradingDay()).isFalse();
                    assertThat(day.source())
                            .isEqualTo(MarketCalendarSource.MANUAL_OVERRIDE);
                });
    }

    @Test
    void loadsAuditsNewestFirstWithinDateRange() {
        LocalDate date = LocalDate.of(2026, 8, 17);
        auditPort.save(audit(date, "first", Instant.parse("2026-06-12T00:00:00Z")));
        auditPort.save(audit(date, "second", Instant.parse("2026-06-12T01:00:00Z")));

        assertThat(auditPort.findBetween(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        )).extracting(MarketCalendarDayAudit::reason)
                .containsExactly("second", "first");
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
                tradingDay ? null : "TEMPORARY_CLOSURE",
                source
        );
    }

    private static MarketCalendarDayAudit audit(
            LocalDate date,
            String reason,
            Instant createdAt
    ) {
        return new MarketCalendarDayAudit(
                null,
                MarketCalendarDay.KRX_STOCK,
                date,
                true,
                false,
                null,
                "TEMPORARY_CLOSURE",
                reason,
                "operator",
                createdAt
        );
    }
}
